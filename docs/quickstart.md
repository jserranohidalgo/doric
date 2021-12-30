---
title: Doric Documentation
permalink: docs/
---

# Quick start

## Installation

To use doric, just add the following dependency in your favourite build tool:

#### Sbt
```scala
libraryDependencies += "org.hablapps" % "doric_2.12" % "0.0.1"
```
#### Maven
```xml
<dependency>
  <groupId>org.hablapps</groupId>
  <artifactId>doric_2.12</artifactId>
  <version>0.0.1</version>
</dependency>
```

Doric is committed to use the most modern APIs first.
* Doric is compatible with Spark version 3.1.2.
* The latest stable version of doric is 0.0.1.
* The latest experimental version of doric is 0.0.0+155-58460d4f-SNAPSHOT.

## Type-safe column expressions

Doric is very easy to work with. First, you require the following import clause:

```scala
import doric._
```

There is no problem in combining conventional Spark column expressions and doric columns. 
However, to avoid name clashes, we will use the prefix `f` for the former ones:

```scala
import org.apache.spark.sql.{functions => f}
``` 

Next, make sure to activate the flag `spark.sql.datetime.java8API.enabled` when creating the Spark context.


The overall purpose of doric is providing a type-safe API on top of the DataFrame API. This essentially means 
that we aim at capturing errors at compile time. For instance, in Spark we can't mix apples and oranges, but this 
code still compiles:
```scala
def df = List(1,2,3).toDF.select($"value" * f.lit(true))
```
It's only when we try to construct the DataFrame that an exception is raised at _run-time_:
```scala
df
// org.apache.spark.sql.AnalysisException: cannot resolve '(`value` * true)' due to data type mismatch: differing types in '(`value` * true)' (int and boolean).;
// 'Project [(value#382 * true) AS (value * true)#386]
// +- LocalRelation [value#382]
// 
// 	at org.apache.spark.sql.catalyst.analysis.package$AnalysisErrorAt.failAnalysis(package.scala:42)
// 	at org.apache.spark.sql.catalyst.analysis.CheckAnalysis$$anonfun$$nestedInanonfun$checkAnalysis$1$2.applyOrElse(CheckAnalysis.scala:161)
// 	at org.apache.spark.sql.catalyst.analysis.CheckAnalysis$$anonfun$$nestedInanonfun$checkAnalysis$1$2.applyOrElse(CheckAnalysis.scala:152)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$transformUp$2(TreeNode.scala:342)
// 	at org.apache.spark.sql.catalyst.trees.CurrentOrigin$.withOrigin(TreeNode.scala:74)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.transformUp(TreeNode.scala:342)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$transformUp$1(TreeNode.scala:339)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$mapChildren$1(TreeNode.scala:408)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.mapProductIterator(TreeNode.scala:244)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.mapChildren(TreeNode.scala:406)
```

Using doric, there is no need to wait for so long: errors will be reported at compile-time!
``` scala mdoc:crash
List(1,2,3).toDF.select(col[Int]("value") * lit(true))
```

As you may see, changes in column expressions are minimal: just annotate column references with the intended type, 
i.e. `col[Int]("name")`, instead of a plain `col("name")`. 
---
**NOTE**

Of course, this only works if you, the programmer, know the intended type 
of the column at compile-time. In a pure dynamic setting, doric is useless. Note, however, that you don't need to know 
in advance the whole row type as with Datasets. In this way, doric sits between a wholehearted static setting and a 
purely dynamic one. It offers type-safety at a minimum cost, without compromising performance, i.e. sticking to DataFrames.
---

Finally, once we have constructed a doric column expression, we can use it within the context of a `withColumn` expression, 
or, in general, wherever we may use plain Spark columns: joins, filters, etc.:

``` scala mdoc
List(1,2,3).toDF.withColumn("other", colInt("value") * lit(1))
```

## Mixing doric and Spark columns

Since doric is intended as a replacement of the _whole_ DataFrame API, type-safe versions of Spark functions 
for numbers, dates, strings, etc., are provided. Occasionally, however, we might need to mix both doric and 
Spark column expressions. There is no problem with that, as this example shows: 

```scala
val strDf = List("hi", "welcome", "to", "doric").toDF("str")
// strDf: org.apache.spark.sql.package.DataFrame = [str: string]

strDf
  .select(f.concat(f.col("str"), f.lit("!!!")) as "newCol") //pure spark
  .select(concat(lit("???"), colString(c"newCol")) as c"finalCol") //pure and sweet doric
  .show()
// +-------------+
// |     finalCol|
// +-------------+
// |     ???hi!!!|
// |???welcome!!!|
// |     ???to!!!|
// |  ???doric!!!|
// +-------------+
//
```

Also, we can transform a pure Spark column into a doric column, and be sure that specific doric validations,
will be applied:
```scala
strDf.select(f.col("str").asDoric[String]).show()
// +-------+
// |    str|
// +-------+
// |     hi|
// |welcome|
// |     to|
// |  doric|
// +-------+
//
```

```scala

strDf.select((f.col("str") + f.lit(true)).asDoric[String]).show
// doric.sem.DoricMultiError: Found 1 error in select
//   cannot resolve '(CAST(`str` AS DOUBLE) + true)' due to data type mismatch: differing types in '(CAST(`str` AS DOUBLE) + true)' (double and boolean).;
//   'Project [(cast(str#391 as double) + true) AS (str + true)#412]
//   +- Project [value#388 AS str#391]
//      +- LocalRelation [value#388]
//   
//   	located at . (quickstart.md:64)
// 
// 	at doric.sem.package$ErrorThrower.$anonfun$returnOrThrow$1(package.scala:9)
// 	at cats.data.Validated.fold(Validated.scala:29)
// 	at doric.sem.package$ErrorThrower.returnOrThrow(package.scala:9)
// 	at doric.sem.TransformOps$DataframeTransformationSyntax.select(TransformOps.scala:139)
// 	at repl.MdocSession$App$$anonfun$7.apply$mcV$sp(quickstart.md:64)
// 	at repl.MdocSession$App$$anonfun$7.apply(quickstart.md:64)
// 	at repl.MdocSession$App$$anonfun$7.apply(quickstart.md:64)
// Caused by: org.apache.spark.sql.AnalysisException: cannot resolve '(CAST(`str` AS DOUBLE) + true)' due to data type mismatch: differing types in '(CAST(`str` AS DOUBLE) + true)' (double and boolean).;
// 'Project [(cast(str#391 as double) + true) AS (str + true)#412]
// +- Project [value#388 AS str#391]
//    +- LocalRelation [value#388]
// 
// 	at org.apache.spark.sql.catalyst.analysis.package$AnalysisErrorAt.failAnalysis(package.scala:42)
// 	at org.apache.spark.sql.catalyst.analysis.CheckAnalysis$$anonfun$$nestedInanonfun$checkAnalysis$1$2.applyOrElse(CheckAnalysis.scala:161)
// 	at org.apache.spark.sql.catalyst.analysis.CheckAnalysis$$anonfun$$nestedInanonfun$checkAnalysis$1$2.applyOrElse(CheckAnalysis.scala:152)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$transformUp$2(TreeNode.scala:342)
// 	at org.apache.spark.sql.catalyst.trees.CurrentOrigin$.withOrigin(TreeNode.scala:74)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.transformUp(TreeNode.scala:342)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$transformUp$1(TreeNode.scala:339)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$mapChildren$1(TreeNode.scala:408)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.mapProductIterator(TreeNode.scala:244)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.mapChildren(TreeNode.scala:406)
```

## Sweet doric syntax sugar

### Column selectors

If you are not used to generic parameters, aliases `colInt`, `colString`, etc., are also available as column selectors.
In this way, we can write `colInt("name")` instead of `col[Int]("name")`. We can't avoid generic parameters when 
selecting arrays, though: `col[Array[Int]]("name")` alias is `colArray[Int]("name")`.

Also, doric provides an interpolator `c"..."`, 
somewhat similar to the `$"..."` Spark interpolator. An expression `c"..."` just returns a column name value of type 
`CName`. This allows us to distinguish between plain string values and column name values. To obtain a doric colum 
from a column name, just add the intended type as follows:

``` scala mdoc
val column: DoricColumn[Int] = c"name"[Int]
``` 

Note also that column names can be implicitly converted to proper doric columns, provided that type information is
available from the context:
``` scala mdoc
val column: DoricColumn[Int] = c"name"
```

### Dot syntax

Doric embraces the dot notation of common idiomatic Scala code wherever possible, instead of the SQL
_method-argument_ style of Spark SQL. For instance, given the following DataFrame: 
```scala
val dfArrays = List(("string", Array(1,2,3))).toDF("str", "arr")
// dfArrays: org.apache.spark.sql.package.DataFrame = [str: string, arr: array<int>]
```

a common transformation in the SQL style would go as follows:
```scala
import org.apache.spark.sql.Column

val sArrCol: Column = f.col("arr")
// sArrCol: Column = arr
val sAddedOne: Column = f.transform(sArrCol, x => x + 1)
// sAddedOne: Column = transform(arr, lambdafunction((x_8 + 1), x_8))
val sAddedAll: Column = f.aggregate(sAddedOne, f.lit(0), (x, y) => x + y)
// sAddedAll: Column = aggregate(transform(arr, lambdafunction((x_8 + 1), x_8)), 0, lambdafunction((x_9 + y_10), x_9, y_10), lambdafunction(x_11, x_11))

dfArrays.select(sAddedAll as "complexTransformation").show
// +---------------------+
// |complexTransformation|
// +---------------------+
// |                    9|
// +---------------------+
//
```

This complex transformation has to be developed in several steps, and usually tested step by step. 
The one-line version is not easy to read, either:

```scala
val complexS: Column = 
    f.aggregate(
        f.transform(f.col("arr"), x => x + 1), 
        f.lit(0), 
        (x, y) => x + y)
// complexS: Column = aggregate(transform(arr, lambdafunction((x_12 + 1), x_12)), 0, lambdafunction((x_13 + y_14), x_13, y_14), lambdafunction(x_15, x_15))

dfArrays.select(complexS as "complexTransformation").show
// +---------------------+
// |complexTransformation|
// +---------------------+
// |                    9|
// +---------------------+
//
```

[//]: # (NOTE JM: why?)

Using doric, it's much easier to keep track of what we can do step-by-step:
```scala
val complexCol: DoricColumn[Int] = 
    col[Array[Int]](c"arr")
      .transform(_ + 1.lit)
      .aggregate(0.lit)(_ + _)
// complexCol: DoricColumn[Int] = TransformationDoricColumn(
//   Kleisli(cats.data.Kleisli$$Lambda$1493/908528661@af23f5)
// )
  
dfArrays.select(complexCol as c"complexTransformation").show
// +---------------------+
// |complexTransformation|
// +---------------------+
// |                    9|
// +---------------------+
//
```

### Literal conversions

Sometimes, Spark allows us to insert literal values to simplify our code: 

```scala
val intDF = List(1,2,3).toDF("int")
// intDF: org.apache.spark.sql.package.DataFrame = [int: int]
val colS = f.col("int") + 1
// colS: Column = (int + 1)

intDF.select(colS).show
// +---------+
// |(int + 1)|
// +---------+
// |        2|
// |        3|
// |        4|
// +---------+
//
```

The default doric syntax is a little stricter and forces us to transform these values to literal columns:

```scala
val colD = colInt(c"int") + 1.lit
// colD: DoricColumn[Int] = TransformationDoricColumn(
//   Kleisli(cats.data.Kleisli$$Lambda$1493/908528661@378ec278)
// )

intDF.select(colD).show
// +---------+
// |(int + 1)|
// +---------+
// |        2|
// |        3|
// |        4|
// +---------+
//
```

In doric, we can also profit from the same literal syntax with the help of implicits. To enable this implicit behaviour,
however, we have to _explicitly_ add the following import statement: 

```scala
import _root_.doric.implicitConversions.literalConversion
val colSugarD = colInt(c"int") + 1
// colSugarD: DoricColumn[Int] = TransformationDoricColumn(
//   Kleisli(cats.data.Kleisli$$Lambda$1493/908528661@2cd28c84)
// )
val columConcatLiterals = concat("this", "is","doric") // concat expects DoricColumn[String] values, the conversion puts them as expected
// columConcatLiterals: StringColumn = TransformationDoricColumn(
//   Kleisli(cats.data.Kleisli$$Lambda$1493/908528661@53c4b0a9)
// ) // concat expects DoricColumn[String] values, the conversion puts them as expected

intDF.select(colSugarD, columConcatLiterals).show
// +---------+-----------------------+
// |(int + 1)|concat(this, is, doric)|
// +---------+-----------------------+
// |        2|            thisisdoric|
// |        3|            thisisdoric|
// |        4|            thisisdoric|
// +---------+-----------------------+
//
```

Of course, implicit conversions are only in effect if the type of the literal value is valid:
```scala
colInt("int") + 1f //an integer with a float value can't be directly added in doric
// error: type mismatch;
//  found   : String("int")
//  required: doric.CName
//     (which expands to)  doric.CName.Type
// colInt("int") + 1f //an integer with a float value can't be directly added in doric
//        ^^^^^
```
```scala
concat("hi", 5) // expects only strings and an integer is found
// error: type mismatch;
//  found   : Int(5)
//  required: doric.StringColumn
//     (which expands to)  doric.DoricColumn[String]
// concat("hi", 5) // expects only strings and an integer is found
//              ^
```
