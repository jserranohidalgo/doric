package doric
package syntax

import scala.annotation.implicitNotFound

import cats.data.Kleisli
import cats.evidence.Is
import cats.implicits._
import doric.sem.{ColumnTypeError, Location, SparkErrorWrapper}
import doric.types.SparkType

import org.apache.spark.sql.catalyst.expressions.ExtractValue
import org.apache.spark.sql.{Column, Dataset, Row, functions => f}
import shapeless.labelled.FieldType
import shapeless.{::, Generic, HList, HNil, LabelledGeneric, Lazy, Witness}
import scala.jdk.CollectionConverters._
import scala.language.dynamics

protected trait DStructs {

  /**
    * Creates a struct with the columns
    * @group Struct Type
    * @param cols
    *   the columns that will form the struct
    * @return
    *   A DStruct DoricColumn.
    */
  def struct(cols: DoricColumn[_]*): RowColumn = {
    cols.toList.traverse(_.elem).map(c => f.struct(c: _*)).toDC
    // cols.map(_.elem).toList.sequence.map(c => f.struct(c: _*)).toDC
  }

  implicit class DStructOps[T](private val col: DoricColumn[T])(implicit
      st: SparkType.Custom[T, Row]
  ) {

    /**
      * Retrieves the child row of the Struct column
      *
      * @group Struct Type
      * @param subColumnName
      * the column name expected to find in the struct.
      * @param location
      * the location if an error is generated
      * @tparam T2
      * the expected type of the child column.
      * @return
      * a reference to the child column of the provided type.
      */
    def getChild[T2: SparkType](
        subColumnName: String
    )(implicit location: Location): DoricColumn[T2] = {
      (col.elem, subColumnName.lit.elem)
        .mapN((a, b) => (a, b))
        .mapK(toEither)
        .flatMap { case (vcolumn, litVal) =>
          Kleisli[DoricEither, Dataset[_], Column]((df: Dataset[_]) => {
            try {
              if (SparkType[Row].isEqual(vcolumn.expr.dataType)) {
                val subColumn = new Column(
                  ExtractValue(
                    vcolumn.expr,
                    litVal.expr,
                    df.sparkSession.sessionState.analyzer.resolver
                  )
                )
                if (SparkType[T2].isEqual(subColumn.expr.dataType))
                  subColumn.asRight
                else
                  ColumnTypeError(
                    subColumnName,
                    SparkType[T2].dataType,
                    subColumn.expr.dataType
                  ).leftNec
              } else {
                ColumnTypeError(
                  "",
                  SparkType[Row].dataType,
                  vcolumn.expr.dataType
                ).leftNec
              }
            } catch {
              case e: Throwable =>
                SparkErrorWrapper(e).leftNec
            }
          })
        }
        .mapK(toValidated)
        .toDC
    }

    def child: DynamicFieldAccessor[T] = new DynamicFieldAccessor(col)

    /**
      * Converts a column containing a StructType into a JSON string with the specified schema.
      * @throws java.lang.IllegalArgumentException in the case of an unsupported type.
      *
      * @group Struct Type
      * @see org.apache.spark.sql.functions.to_json(e:org\.apache\.spark\.sql\.Column,options:scala\.collection\.immutable\.Map\[java\.lang\.String,java\.lang\.String\]):* org.apache.spark.sql.functions.to_csv
      * @todo scaladoc link (issue #135)
      */
    def toJson(options: Map[String, String] = Map.empty): StringColumn =
      col.elem.map(x => f.to_json(x, options.asJava)).toDC
  }

  class DynamicFieldAccessor[T](dCol: DoricColumn[T])(implicit
      st: SparkType.Custom[T, Row]
  ) extends Dynamic {

    /**
      * Allows for accessing fields of struct columns using the syntax `rowcol.name[T]`.
      * This expression stands for `rowcol.getChild[T](name)`.
      *
      * @param name
      * @param location
      * @param st
      * @tparam A
      * @return The column which refers to the given field
      * @throws doric.sem.ColumnTypeError if the parent column is not a struct
      */
    def selectDynamic[A: SparkType](name: String)(implicit
        location: Location
    ): DoricColumn[A] = dCol.getChild[A](name)
  }

  @annotation.implicitNotFound(msg = "No field ${K} in record ${L}")
  trait SelectorWithSparkType[L <: HList, K <: Symbol] {
    type V
    val st: SparkType[V]
  }

  object SelectorWithSparkType extends SelectorLPI {
    type Aux[L <: HList, K <: Symbol, _V] = SelectorWithSparkType[L, K] {
      type V = _V
    }

    implicit def Found[K <: Symbol, _V: SparkType, T <: HList] =
      new SelectorWithSparkType[FieldType[K, _V] :: T, K] {
        type V = _V
        val st = SparkType[_V]
      }
  }

  trait SelectorLPI {
    implicit def KeepFinding[K1, V1, T <: HList, K <: Symbol](implicit
        T: SelectorWithSparkType[T, K]
    ) =
      new SelectorWithSparkType[FieldType[K1, V1] :: T, K] {
        type V = T.V
        val st = T.st
      }
  }

  implicit class StructOps[T, L <: HList](dc: DoricColumn[T])(implicit
      lg: LabelledGeneric.Aux[T, L],
      st: SparkType.Custom[T, Row]
  ) {
    def get[K <: Symbol](k: Witness)(implicit
        E: k.T =:= K,
        S: Lazy[SelectorWithSparkType[L, K]],
        location: Location
    ): DoricColumn[S.value.V] =
      new DStructOps(dc).getChild[S.value.V](k.value.name)(S.value.st, location)
  }

  @implicitNotFound(
    "Field types of struct type ${T} couldn't be found"
  )
  trait Struct2[T <: Product] {
    type F1
    type F2
    val f1: String
    val f2: String
    def apply(u: DoricColumn[F1], v: DoricColumn[F2]): DoricColumn[T] =
      ((u.elem, v.elem).mapN((x, y) => f.struct(x.as(f1), y.as(f2)))).toDC
  }

  object Struct2 {
    type Aux[T <: Product, _F1, _F2] = Struct2[T] {
      type F1 = _F1; type F2 = _F2
    }

    implicit def record2[K1 <: Symbol, K2 <: Symbol, V1, V2, L, T <: Product](
        implicit
        LG: Lazy[LabelledGeneric.Aux[T, FieldType[K1, V1] :: FieldType[
          K2,
          V2
        ] :: HNil]],
        W1: Witness.Aux[K1],
        W2: Witness.Aux[K2]
    ) =
      new Struct2[T] {
        type F1 = V1
        type F2 = V2
        val f1 = W1.value.name
        val f2 = W2.value.name
      }

    def apply[T <: Product]: Struct2Aux[T] =
      new Struct2Aux[T] {}

    trait Struct2Aux[T <: Product] {
      def apply[U, V](u: DoricColumn[U], v: DoricColumn[V])(implicit
          /* According to https://www.scala-lang.org/api/2.13.8/scala/annotation/implicitNotFound.html
                                                            this should compile:
                                                            @implicitNotFound(
                                                              "Struct type ${T} can't be decomposed into fields of types ${U} and ${V}"
                                                            )*/
          S2: Struct2.Aux[T, U, V]
      ): DoricColumn[T] =
        S2(u, v)

    }

    def unapply[A: SparkType, B: SparkType](t: DoricColumn[(A, B)])(implicit
        ST: SparkType.Custom[(A, B), Row]
    ): Option[(DoricColumn[A], DoricColumn[B])] =
      Some((t.getChild("_1"), t.getChild("_2")))
  }

  implicit def fromTuple2[A, B, S <% DoricColumn[A], T <% DoricColumn[B]](
      t: (S, T)
  ): DoricColumn[(A, B)] =
    Struct2[(A, B)](t._1, t._2)
}
