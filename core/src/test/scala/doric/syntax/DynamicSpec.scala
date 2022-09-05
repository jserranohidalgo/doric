package doric
package syntax

import doric.types.SparkType
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers

import org.apache.spark.sql.Row

class DynamicSpec extends DoricTestElements with EitherValues with Matchers {

  import spark.implicits._

  private val df = List((User("John", "doe", 34), 1))
    .toDF("user", "delete")

  describe("Dynamic-macro invocations") {

    describe("over generic struct types, i.e. Rows") {

      it("should work if type expectations are provided") {
        df.validateColumnType(row.user[Row])
        df.validateColumnType(row.user[User])
        df.validateColumnType(row.user[Row].name[String]())
        df.validateColumnType(col[Row]("user").age[Int]())
      }

      it("should fail if type annotations are not provided") {
        // TODO: implement with macro
        // """row.name""" shouldNot compile // Error should be "Type of field `name` should be specified"
        """row.user[Row].name()""" shouldNot compile
      }

    }

    describe("over fully specified struct types, i.e. case classes") {
      // SparkType[(User, Int)]
      it("should work without type expectations") {
        // df.validateColumnType(row[(User, Int)]._1: DoricColumn[User])
        // df.validateColumnType(row[(User, Int)]._2: DoricColumn[Int])

        df.validateColumnType(row.user[User].name: DoricColumn[String])
        // df.validateColumnType(row.user[User].age: DoricColumn[Int])
      }

      it(
        "should work with type expectations if they agree with the column type"
      ) {
        // df.validateColumnType(row[(User, Int)]._1[User])
        // df.validateColumnType(row[(User, Int)]._2[Int])
        /*df.validateColumnType(row.user[User].name[String]())
        df.validateColumnType(row.user[User].age[Int]())*/
      }

      it(
        "should fail with type expectations if they don't agree with the column type"
      ) {
        // """row[(User, Integer)]._1[Int]""" shouldNot compile // Error should be "type of member `_1` is `User`, not `Int`
        // """row[(User, Integer)]._2[User]""" shouldNot compile
        // """row.user[User].name[Int]""" shouldNot compile
        // """row.user[User].age[String]""" shouldNot compile
      }
    }
    /*
    describe("over non-struct types") {

      it("should fail") {
        // """row[Int].name""" shouldNot compile // Error should be "Cannot resolve symbol `name` as member of type `Int`"
        // """row._1.name.first""" shouldNot compile // Error should be "Cannot resolve symbol `first` as member of type `String`"
      }
    } */
  }

}
