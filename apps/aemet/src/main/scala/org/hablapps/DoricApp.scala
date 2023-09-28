package org.hablapps

import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import doric._

object DoricApp extends Commons {

  override def run(estacionesDf: DataFrame, datosMeteoDf: DataFrame, calidadAireMadrid: DataFrame)
         (implicit spark: SparkSession): Unit = {

    val estacionCol = colString("estacion")
    val estacion2Col = colInt("estacion")
    estacionesDf.select(estacionCol.split(" ".lit)).show(2, false)
    datosMeteoDf.select(estacion2Col).show(2, false)

    val meteoDf = {
      val vCols = datosMeteoDf.columns.toList.filter(_.matches("v\\d+")).map(colString)
      val dCols = datosMeteoDf.columns.toList.filter(_.matches("d\\d+")).map(colDouble)
      val otherCols: List[IntegerColumn] = List("provincia", "municipio", "estacion", "ano", "mes").map(colInt)

      def struct2[U, V](u: DoricColumn[U], v: DoricColumn[V]): DoricColumn[(U, V)] = ???

      val zippedArrays: ArrayColumn[(String, Double)] = array(vCols: _*)
        .zipWith(array(dCols: _*))(struct2[String, Double])
/*
      val zippedArrays: ArrayColumn[Row] = array(vCols: _*)
        .zipWith(array(dCols: _*))(
          (x, y) => struct(x.as("v"), y.as("d"))
        )
*/
      val myMapCol: MapColumn[String, (String, Double)] = array(dCols.map(_.columnName.lit): _*)
        .mapFromArrays(
          zippedArrays
        ).as("myMap")

      datosMeteoDf.select(
        otherCols
          :+ myMapCol
          : _*
      )
    }

    meteoDf.show(3)
    meteoDf.printSchema()

    val estacionesAndMeteoDf = estacionesDf.join(meteoDf, "left", LeftDF.colInt("codigo_corto") === RightDF.colInt("estacion"))

    estacionesAndMeteoDf.show(3)

    val calidadAireDf = {
      val vCols = calidadAireMadrid.columns.toList.filter(_.matches("v\\d+")).map(colString)
      val dCols = calidadAireMadrid.columns.toList.filter(_.matches("d\\d+")).map(colDouble)
      val otherCols: List[IntegerColumn] = List("provincia", "municipio", /*"estacion",*/ "magnitud", /*"punto_muestreo",*/ "ano", "mes").map(colInt)

      val zippedArrays: ArrayColumn[Row] = array(vCols: _*)
        .zipWith(array(dCols: _*))(
          (x, y) => struct(x.as("v"), y.as("d"))
        )

      val myMapCol: MapColumn[String, Row] = array(dCols.map(_.columnName.lit): _*)
        .mapFromArrays(
          zippedArrays
        ).as("myMap")

      import doric.implicitConversions._
      datosMeteoDf.select(
        otherCols
          :+ colString("punto_muestreo").substring(0.lit, 8).unsafeCast[Int].as("codigo")
          :+ myMapCol
          : _*
      )
    }

    estacionesAndMeteoDf.printSchema()

    estacionesAndMeteoDf.join(calidadAireDf, "left", colInt("codigo"), colInt("ano"), colInt("mes"))
      .drop(calidadAireDf("codigo"))
      .filter(colInt("codigo").isNotNull)
      .show(3)
  }

}
