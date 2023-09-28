import org.apache.spark.sql.SparkSession

object Main {
  val spark: SparkSession = SparkSession
    .builder()
    .master("local[*]")
    .getOrCreate()

  def main(args: Array[String]): Unit = {
//    ConstDomProcessor(spark, 1990 to 1999).show()
    ConsistencyProcessor(spark, 2012).show(30)
  }
}