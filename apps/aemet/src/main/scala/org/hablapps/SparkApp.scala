package org.hablapps

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object SparkApp extends Commons {

  override def run(estacionesDf: DataFrame, datosMeteoDf: DataFrame, calidadAireMadrid: DataFrame)
                  (implicit spark: SparkSession): Unit = {

    datosMeteoDf.show(3)
    datosMeteoDf.printSchema

    val estacionCol = col("estacion")
    estacionesDf.select(split(estacionCol, " ")).show(2, false)
    datosMeteoDf.select(split(estacionCol, " ")).show(2, false)

    val meteoDf = {
      // val mapCols = datosMeteoDf.columns.filter(_.matches("(v\\d+)|(d\\d+)")).toList
      // datosMeteoDf.select(
      //   datosMeteoDf.columns.diff(mapCols).map(col)
      //     :+ map_from_entries(array(mapCols.head, mapCols.tail:_*)).as("myMap"): _*
      // ).show(3, false)

      // println(vCols.mkString(", "))
      // println(dCols.mkString(", "))
      // println(otherCols.mkString(", "))

      val vCols = datosMeteoDf.columns.filter(_.matches("v\\d+")).toList
      val dCols = datosMeteoDf.columns.filter(_.matches("d\\d+")).toList
      val otherCols = List("provincia", "municipio", "estacion", "ano", "mes")

      datosMeteoDf.select(
        otherCols.map(col)
          :+ map_from_arrays(
          array(dCols.map(lit): _*),
          zip_with(
            array(vCols.head, vCols.tail: _*),
            array(dCols.head, dCols.tail: _*),
            (x, y) => struct(x.as("v"), y.as("d"))
          )
        ).as("myMap")
          : _*
      )
    }

    meteoDf.show(3)
    meteoDf.printSchema()

    // meteoDf.join(estacionesDf, meteoDf("estacion") === estacionesDf("codigo_corto"), "left")
    //   .show(3)

    estacionesDf.printSchema()

    val estacionesAndMeteoDf = estacionesDf.join(meteoDf, meteoDf("estacion") === estacionesDf("codigo_corto"), "left")

    estacionesAndMeteoDf.show(3)

    val calidadAireDf = {
      val vCols = calidadAireMadrid.columns.filter(_.matches("v\\d+")).toList
      val dCols = calidadAireMadrid.columns.filter(_.matches("d\\d+")).toList
      val otherCols = List("provincia", "municipio", /*"estacion",*/ "magnitud", /*"punto_muestreo",*/ "ano", "mes")

      calidadAireMadrid.select(
        otherCols.map(col)
          // calidadAireMadrid("punto_muestreo").substr(0, 8),
          // substring(calidadAireMadrid("punto_muestreo"), 0, 8)
          :+ col("punto_muestreo").substr(0, 8).as("codigo")
          :+ map_from_arrays(
          array(dCols.map(lit): _*),
          zip_with(
            array(vCols.head, vCols.tail: _*),
            array(dCols.head, dCols.tail: _*),
            (x, y) => struct(x.as("v"), y.as("d"))
          )
        ).as("myMap")
          : _*
      )
    }

    calidadAireDf.show(3)

    estacionesAndMeteoDf.select("codigo").printSchema
    calidadAireDf.select("codigo").printSchema

    estacionesAndMeteoDf.join(calidadAireDf, Seq("codigo", "ano", "mes"), "left")
      .filter(col("codigo").isNotNull)
      .show(3)
  }

}
