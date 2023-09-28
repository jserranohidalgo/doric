import org.apache.spark.sql.{DataFrame, SparkSession}
import doric.implicitConversions.literalConversion
import doric._


object ConstDomProcessor {

  def apply(spark: SparkSession, seasons: Range.Inclusive): DataFrame = {
    this.apply(spark, seasons: _*)
  }


  def apply(spark: SparkSession, seasons: Int *): DataFrame = {

    val allRaces = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/races.csv")
      // filtro las temporadas que me interesan
      .where(colInt("year").isIn(seasons:_*))

    // me quedo unicamente con las ultimas carreras de cada temporada
    val maxRounds = allRaces
      .groupBy("year")
      .agg(max(colInt("round")).as("round"), last(colInt("raceId")).as("raceId"))
      .select("year", "raceId")

    val lastRaces = allRaces
      .innerJoinKeepLeftKeys(maxRounds, colInt("raceId"), colInt("year"))
      .select("raceId", "year")

    val constructor_standings = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/constructor_standings.csv")

    val constructors = spark.read.format("csv")
      .option("header", "true")
      .option("sep", ",")
      .option("inferSchema" , "true")
      .load("data/constructors.csv")

    constructorQuery(lastRaces, constructor_standings, constructors)
  }

  def constructorQuery(lastRaces: DataFrame,
                       constructor_standings: DataFrame,
                       constructors: DataFrame
                      ): DataFrame = {

    val constructorMap = constructors
      .select("constructorId", "name")

    val finalDf = constructor_standings
      .join(lastRaces, Seq("raceId"), "right")
      .where(colInt("position") === 1)

    val championshipsByConstructor = finalDf
      .groupBy("constructorId")
      .agg(count(colInt("constructorId")).as("totalChampWins"))

    val winsByConstructor = finalDf
      .groupBy("constructorId")
      .agg(sum(colInt("wins")).as("totalRaceWins"))

    finalDf
      .innerJoinKeepLeftKeys(championshipsByConstructor, colInt("constructorId"))
      .innerJoinKeepLeftKeys(winsByConstructor, colInt("constructorId"))
      .dropDuplicates("constructorId")
      .join(constructorMap, "constructorId")
      .select("totalChampWins", "totalRaceWins", "name")
      .orderBy(colLong("totalChampWins").desc, colLong("totalRaceWins").desc)

  }
}