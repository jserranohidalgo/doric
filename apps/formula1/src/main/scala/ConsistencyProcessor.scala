import org.apache.spark.sql.{DataFrame, SparkSession}
import doric.implicitConversions.literalConversion
import doric._
import org.apache.spark.sql.{functions => f}

object ConsistencyProcessor {

  def apply(spark: SparkSession, seasons: Range.Inclusive): DataFrame = {
    this.apply(spark, seasons: _*)
  }


  def apply(spark: SparkSession, seasons: Int*): DataFrame = {
    val races = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/races.csv")
      // para filtrar por temporadas hay que hacerlo por la columna "year"
      .where(colInt("year").isIn(seasons:_*))

    computeConsistency(spark, races)
  }

  def apply(spark: SparkSession): DataFrame = {
    val races = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/races.csv")
      .where(colInt("year") >= 1996)

    computeConsistency(spark, races)
  }

  private def computeConsistency(spark: SparkSession, races: DataFrame): DataFrame = {
    import spark.implicits._

//    val lapTimeToMs = (time: String) => {
//      val regex = """(\d|\d\d):(\d\d)\.(\d\d\d)""".r
//      time match {
//        case regex(min,sec,ms) => min.toInt * 60 * 1000 + sec.toInt * 1000 + ms.toInt
//        case "\\N" => 180000
//      }
//    }: Long

//    val lapTimeToMsUDF = f.udf(lapTimeToMs)
//    spark.udf.register("lapTimeToMs", lapTimeToMsUDF)

    val regex = """(\d|\d\d):(\d\d)\.(\d\d\d)"""

    def extractTimeFromColumn(column: DoricColumn[String]) =
      (column.regexpExtract(regex, 1).unsafeCast[Int],
      column.regexpExtract(regex, 2).unsafeCast[Int],
      column.regexpExtract(regex, 3).unsafeCast[Int])

    def lapTimeToMs(column: DoricColumn[String]): DoricColumn[Int] = {
      val (min, sec, ms) = extractTimeFromColumn(column)
      min * 60 * 1000 + sec * 1000 + ms
    }


    val msToLapTime = (time: Long) => {
      val mins = time / 60000
      val secs = (time - mins * 60000) / 1000
      val ms = time - mins * 60000 - secs * 1000

      val formattedSecs = if ((secs / 10).toInt == 0) "0" + secs else secs
      // if ms = 00x -> "0"+"0"+x . if ms = 0xx -> "0"+ms
      val formattedMs =
        if ((ms / 100).toInt == 0) "0" +
          (if ((ms / 10).toInt == 0) "0" + ms else ms)
        else ms
      mins + ":" + formattedSecs + "." + formattedMs
    }: String

    val msToLapTimeUDF = f.udf(msToLapTime)
    spark.udf.register("msToLapTime", msToLapTimeUDF)


    val lapsByRace = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/lap_times.csv")
      .withColumnRenamed("time", "lapTime")
      // filtro las vueltas de las carreras en el periodo de tiempo dado
      .join(races, Seq("raceId"), "right")

    val unnecesaryDf = lapsByRace
      // media de tiempos de vuelta por piloto
      .groupBy("driverId")
      .agg(avg(colInt("milliseconds")).as("avgMs"))

    val avgLapTimes = lapsByRace
      .innerJoinKeepLeftKeys(unnecesaryDf, colInt("driverId"))
      .dropDuplicates("driverId")
      .select("driverId", "avgMs")


    val drivers = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/drivers.csv")


    val filteredLapTimes = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/lap_times.csv")
      .join(races, Seq("raceId"), "right")

    val unnecesaryDf2 = filteredLapTimes
      .groupBy("driverId")
      .agg(count(colInt("lap")).as("lapsPerDriver"))

    val lapCount = filteredLapTimes
      .innerJoinKeepLeftKeys(unnecesaryDf2, colInt("driverId"))


    // saco la media de vueltas dadas en este periodo de tiempo
    val (distinctDrivers, allLaps) = lapCount
      .agg(
        f.countDistinct("driverId"),
        f.count(f.col("lap"))
      ).as[(BigInt, BigInt)]
      .head

    val avgLapsThisPeriod = allLaps.toInt / distinctDrivers.toInt

    //filtrar con isInCollection es igual a hacer un join left-anti y mejora el rendimiento
    val experiencedDrivers = lapCount
      // filtro los pilotos mas experimentados
      .where(colLong("lapsPerDriver") >= avgLapsThisPeriod.toLong)
      .select("driverId")
      .distinct()
      .as[Int]
      .collect()

    val filteredResultsWLapTime = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/results.csv")
      // filtro por temporada
      .join(races, Seq("raceId"), "right")
      .na.drop(Seq("fastestLapTime"))
      // paso la vuelta rapida de tiempo por vuelta a ms
      .withColumn("fastestLapTimeMs", lapTimeToMs(colString("fastestLapTime")))

    // saco la media de vueltas rapidas
    val avgFastestLap = filteredResultsWLapTime
      .groupBy("driverId")
      .agg(avg(colInt("fastestLapTimeMs")).as("avgFastestLapMs"))

    filteredResultsWLapTime
      .innerJoinKeepLeftKeys(avgFastestLap, colInt("driverId"))
      .dropDuplicates("driverId")
      .join(avgLapTimes, Seq("driverId"), "left")
      // saco el diferencial
      .withNamedColumns((colDouble("avgMs") - colDouble("avgFastestLapMs")).abs.as("diffLapTimes"))
      // vuelvo a pasar a tiempo de vuelta
      .withColumn("avgDiff", msToLapTimeUDF(f.col("diffLapTimes")))
      // filtro pilotos "experimentados"
      .where(colInt("driverId").isIn(experiencedDrivers:_*))
      // concateno el nombre y apellido de los pilotos
      .join(drivers, "driverId")
      .withColumn("driver", concat(col("forename"), lit(" "), col("surname")))
      .select("driver", "avgDiff")
      .orderBy("avgDiff")
  }
}
