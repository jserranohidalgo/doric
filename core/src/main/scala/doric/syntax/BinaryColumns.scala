package doric
package syntax

import cats.implicits.{catsSyntaxTuple2Semigroupal, toTraverseOps}
import doric.types.{BinaryType, SparkType}
import org.apache.spark.sql.catalyst.expressions.Decode
import org.apache.spark.sql.{Column, functions => f}

private[syntax] trait BinaryColumns {

  /**
    * Concatenates multiple binary columns together into a single column.
    *
    * @group Binary Type
    * @param col
    *   the first binary column
    * @param cols
    *   the binary columns
    * @return
    *   Doric Column with the concatenation.
    * @see [[org.apache.spark.sql.functions.concat]]
    */
  def concatBinary(
      col: BinaryColumn,
      cols: BinaryColumn*
  ): BinaryColumn =
    (col +: cols).toList.traverse(_.elem).map(f.concat(_: _*)).toDC

  implicit class BinaryOperationsSyntax[T: BinaryType: SparkType](
      column: DoricColumn[T]
  ) {

    /**
      * Calculates the MD5 digest of a binary column and returns the value
      * as a 32 character hex string.
      *
      * @group Binary Type
      * @see [[org.apache.spark.sql.functions.md5]]
      */
    def md5: StringColumn = column.elem.map(f.md5).toDC

    /**
      * Calculates the SHA-1 digest of a binary column and returns the value
      * as a 40 character hex string.
      *
      * @group Binary Type
      * @see [[org.apache.spark.sql.functions.sha1]]
      */
    def sha1: StringColumn = column.elem.map(f.sha1).toDC

    /**
      * Calculates the SHA-2 family of hash functions of a binary column and
      * returns the value as a hex string.
      *
      * @throws java.lang.IllegalArgumentException if numBits is not in the permitted values
      * @group Binary Type
      * @see [[org.apache.spark.sql.functions.sha2]]
      */
    def sha2(numBits: Int): StringColumn =
      column.elem.map(x => f.sha2(x, numBits)).toDC

    /**
      * Calculates the cyclic redundancy check value (CRC32) of a binary column and
      * returns the value as a long column.
      *
      * @group Binary Type
      * @see [[org.apache.spark.sql.functions.crc32]]
      */
    def crc32: LongColumn = column.elem.map(f.crc32).toDC

    /**
      * Computes the BASE64 encoding of a binary column and returns it as a string column.
      * This is the reverse of unbase64.
      *
      * @group Binary Type
      * @see [[org.apache.spark.sql.functions.base64]]
      */
    def base64: StringColumn = column.elem.map(f.base64).toDC

    /**
      * Computes the first argument into a string from a binary using the provided character set
      * (one of 'US-ASCII', 'ISO-8859-1', 'UTF-8', 'UTF-16BE', 'UTF-16LE', 'UTF-16').
      * If either argument is null, the result will also be null.
      *
      * @group Binary Type
      * @see [[org.apache.spark.sql.functions.decode]]
      */
    def decode(charset: StringColumn): StringColumn =
      (column.elem, charset.elem)
        .mapN((col, char) => {
          new Column(Decode(col.expr, char.expr))
        })
        .toDC
  }

}
