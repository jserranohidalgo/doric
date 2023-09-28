package org.hablapps

import java.util.TimeZone
import org.apache.log4j.{Level, Logger}

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.col

trait Commons {

  implicit lazy val sparkSession: SparkSession = {
    Logger.getLogger("org").setLevel(Level.OFF)

    val timeZone: String = "UTC"
    TimeZone.setDefault(TimeZone.getTimeZone(timeZone))

    val ss = SparkSession
      .builder()
      .master("local")
      .config("spark.sql.session.timeZone", timeZone)
      .config("spark.sql.datetime.java8API.enabled", value = true)
      .appName("doric-show")
      .enableHiveSupport()
      .getOrCreate()

    ss.sparkContext.setLogLevel("ERROR")
    ss
  }

  private def readCsvFromResources(folder: String)(implicit spark: SparkSession): DataFrame = {
    val resourcePath = getClass.getResource("/data").getPath
    val inputDf = spark.read
      .options(Map(
        "header" -> "true",
        "delimiter" -> ";",
        "inferSchema" -> "true",
        "encoding" -> "ISO-8859-1",
        // "recursiveFileLookup" -> "true"
      ))
      .csv(s"$resourcePath/$folder/*.csv")

    val normalizedColumns = inputDf.columns.map(normalizeString).toMap

    inputDf.withColumnsRenamed(normalizedColumns)
  }

  private def normalizeString(input: String): (String, String) = {

    val normalizedString = input
      .toLowerCase
      .replaceAll("[áàâä]", "a")
      .replaceAll("[éèêë]", "e")
      .replaceAll("[íìîï]", "i")
      .replaceAll("[óòôö]", "o")
      .replaceAll("[úùûü]", "u")
      .replaceAll("ç", "c")
      .replaceAll("ñ", "n")
      .replaceAll("\\s", "_")
      .replaceAll("\\W", "")

    input -> normalizedString.toLowerCase
  }

  def main(args: Array[String]): Unit = {
    val estacionesDf = readCsvFromResources("estaciones")
      .select("codigo", "codigo_corto", "estacion", "direccion")
      .withColumn("codigo", col("codigo").cast("int"))
    val datosMeteoDf = readCsvFromResources("datos_meteo")
    val calidadAireMadrid = readCsvFromResources("calidad_aire_madrid")
      .filter(col("ano") >= 2019)

    run(estacionesDf, datosMeteoDf, calidadAireMadrid)
  }

  def run(estacionesDf: DataFrame, datosMeteoDf: DataFrame, calidadAireMadrid: DataFrame)
         (implicit spark: SparkSession): Unit

}
