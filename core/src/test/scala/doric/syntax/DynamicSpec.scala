package doric
package syntax

import doric.types.SparkType
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers

import org.apache.spark.sql.Row

class DynamicSpec extends DoricTestElements with EitherValues with Matchers {

  import spark.implicits._

  private val df = List((User("John", "doe", 34), ("John", 1), 1))
    .toDF("user", "tuple", "delete")

  describe("Dynamic-macro invocations") {

    describe("over non-struct types") {

      it("should fail") {
        """row.int[Int].name()""" shouldNot compile
        """row._1[User].name().first()""" shouldNot compile
      }
    }

    describe("over generic struct types, i.e. Rows") {

      it("should fail if type annotations are not provided") {
        // TODO: implement with macro
        row.name
        // """row.name""" shouldNot compile // Error should be "Type of field `name` should be specified"
        """row.user[Row].name()""" shouldNot compile
      }

      it("should work if type expectations are provided") {
        df.validateColumnType(row.user[Row])
        df.validateColumnType(row.user[User])
        df.validateColumnType(row.user[Row].name[String]())
        df.validateColumnType(row.user[Row].age[Int]())
      }
    }

    describe("over fully specified struct types, i.e. case classes") {

      it(
        "should not work with or without type expectations if member doesn't exist"
      ) {
        /*row.tuple[(String, Int)]._3()
        row.user[User].ageee()
        row.tuple[(String, Int)]._3[String]()*/
        """row.tuple[(String, Int)]._3()""" shouldNot compile
        """row.user[User].ageee()""" shouldNot compile
        """row.tuple[(String, Int)]._3[String]()""" shouldNot compile
        """row.user[User].ageee[Int]()""" shouldNot compile
      }

      it("should work without type expectations if member exists") {
        df.validateColumnType(row.tuple[(String, Int)]._1())
        df.validateColumnType(row.user[User].name())
        df.validateColumnType(row.user[User].age())
      }

      it(
        "should work with type expectations if they agree with the column type"
      ) {
        df.validateColumnType(row.tuple[(String, Int)]._1[String]())
        df.validateColumnType(row.tuple[(String, Int)]._2[Int]())
        df.validateColumnType(row.user[User].name[String]())
        df.validateColumnType(row.user[User].age[Int]())
      }

      it(
        "should fail with type expectations if they don't agree with the column type"
      ) {
        """row.table[(String, Int)]._1[Int]()""" shouldNot compile
        """row.table[(String, Int)]._2[String]()""" shouldNot compile
        """row.user[User].name[Int]()""" shouldNot compile
        """row.user[User].age[String]()""" shouldNot compile
      }
    }
  }
}
