---
title: Doric Documentation
permalink: docs/
---

# Quick start

## Installation

To use doric, just add the following dependency in your favourite build tool:

#### Sbt
```scala
libraryDependencies += "org.hablapps" % "doric_2.12" % "@STABLE_VERSION@"
```
#### Maven
```xml
<dependency>
  <groupId>org.hablapps</groupId>
  <artifactId>doric_2.12</artifactId>
  <version>@STABLE_VERSION@</version>
</dependency>
```

Doric is committed to use the most modern APIs first.
* Doric is compatible with Spark version @SPARK_VERSION@.
* The latest stable version of doric is @STABLE_VERSION@.
* The latest experimental version of doric is @VERSION@.

## Type-safe column expressions

Doric is very easy to work with. First, you require the following import clause:

```scala mdoc
import doric._
```

There is no problem in combining conventional Spark column expressions and doric columns. 
However, to avoid name clashes, we will use the prefix `f` for the former ones:

``` scala mdoc
import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.{functions => f}
``` 

Next, make sure to activate the flag `spark.sql.datetime.java8API.enabled` when creating the Spark context.

```scala mdoc:invisible
val spark = doric.DocInit.getSpark
```

The overall purpose of doric is providing a type-safe API on top of the DataFrame API. This essentially means 
that we aim at capturing errors at compile time. For instance, in Spark we can't mix apples and oranges, but this 
code compiles:
```scala mdoc
def df = List(1,2,3).toDF.select($"value" * f.lit(true))
```
It's only when we try to construct the DataFrame that an exception is raised at _run-time_:
```scala mdoc:crash
df
``` 

Using doric, there is no need to wait for so long: errors will be reported at compile-time!
``` scala mdoc:crash
def df = List(1,2,3).toDF.select(col[Int]("value") * lit(true))
```

As you may see, changes in column expressions are minimal: just annotate column references with the intended type, 
i.e. `col[Int]("name")`, instead of a plain `col("name")`. If you are not used to generic parameters, 
aliases `colInt`, `colString`, etc., are also available. Last, doric also provides an interpolator `c"..."`, somewhat
similar to the `$"..."` Spark interpolator. An expression `c"..."` just returns a column name value of type `CName`. This 
allows us to distinguish between plain string values and column name values. To obtain a doric colum from a column
name, just add the intended type as follows: 

``` scala mdoc
val column: DoricColumn[Int] = c"name"[Int]
``` 

Note also that column names can be implicitly converted to proper doric columns, provided that the type information is 
available from the context: 
``` scala mdoc
val column: DoricColumn[Int] = c"name"
```

---
**NOTE**

Whatever the means chosen to construct doric columns, this only works if you, the programmer, know the intended type 
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

Doric is intended as a replacement of the _whole_ DataFrame API, so that type-safe versions of numerical, date, 
string, etc., Spark functions are provided. Occasionally, however, we might need to mix both doric and Spark column 
expressions. There is no problem at all, as this example shows: 

```scala mdoc
import org.apache.spark.sql.{functions => f}
df
  .select(f.concat(f.col("str"), f.lit("!!!")) as "newCol") //pure spark
  .select(concat(lit("???"), colString(c"newCol")) as c"finalCol") //pure and sweet doric
  .show()
```

Also, if you don't want to use doric to transform a column, you can transform a pure spark column into doric, and be sure that the type of the transformation is ok.
```scala mdoc
df.select(f.col("str").asDoric[String]).show()
```

But we recommend to use always the columns selectors from doric to prevent errors that doric can detect in compile time
```scala mdoc:crash
val sparkToDoricColumn = (f.col("str") + f.lit(true)).asDoric[String]
df.select(sparkToDoricColumn).show
```

In spark the sum of a string with a boolean will throw an error in runtime. In doric this code won't be able to compile.
```scala mdoc:fail
col[String](c"str") + true.lit
```

## Sweet doric syntax sugar
### Column selector alias
We know that doric can be seen as an extra boilerplate to get the columns, that's why we provide some extra methods to acquire the columns.
```scala mdoc
colString(c"str") // similar to col[String]("str")
colInt(c"int") // similar to col[Int]("int")
colArray[Int](c"int") // similar to col[Array[Int]]("int")
```
### Readable syntax
Doric tries to be less SQL verbose, and adopt a more object-oriented API, allowing the developer to view with the dot notation of scala the methods that can be used.
```scala mdoc
val dfArrays = List(("string", Array(1,2,3))).toDF("str", "arr")
```
Spark SQL method-argument way to develop
```scala mdoc
import org.apache.spark.sql.Column

val sArrCol: Column = f.col("arr")
val sAddedOne: Column = f.transform(sArrCol, x => x + 1)
val sAddedAll: Column = f.aggregate(sArrCol, f.lit(0), (x, y) => x + y)

dfArrays.select(sAddedAll as "complexTransformation").show
```
This complex transformation has to ve developed in steps, and usually tested step by step. Also, the one line version is not easy to read
```scala mdoc
val complexS = f.aggregate(f.transform(f.col("arr"), x => x + 1), f.lit(0), (x, y) => x + y)

dfArrays.select(complexS as "complexTransformation").show
```

Doric's way
```scala mdoc
val dArrCol: DoricColumn[Array[Int]] = col[Array[Int]](c"arr")
val dAddedOne: DoricColumn[Array[Int]] = dArrCol.transform(x => x + 1.lit)
val dAddedAll: DoricColumn[Int] = dAddedOne.aggregate[Int](0.lit)((x, y) => x + y)

dfArrays.select(dAddedOne as c"complexTransformation").show
```
We know all the time what type of data we will have, so is much easier to keep track of what we can do, and simplify the line o a single:
```scala mdoc
val complexCol: DoricColumn[Int] = col[Array[Int]](c"arr")
  .transform(_ + 1.lit)
  .aggregate(0.lit)(_ + _)
  
dfArrays.select(complexCol as c"complexTransformation").show
```

### Literal conversions
Sometimes spark allows adding direct literal values to simplify code
```scala mdoc

val intDF = List(1,2,3).toDF("int")
val colS = f.col("int") + 1

intDF.select(colS).show
```

Doric is a little stricter, forcing to transform this values to literal columns
```scala mdoc
val colD = colInt(c"int") + 1.lit

intDF.select(colD).show
```

This is de basic flavor to work with doric, but this obvious transformations can be simplified if we import an implicit conversion
```scala mdoc
import doric.implicitConversions.literalConversion
val colSugarD = colInt(c"int") + 1
val columConcatLiterals = concat("this", "is","doric") // concat expects DoricColumn[String] values, the conversion puts them as expected

intDF.select(colSugarD, columConcatLiterals).show
```

This conversion will transform any pure scala value, to its representation in a doric column, only if the type is valid
```scala mdoc:fail
colInt("int") + 1f //an integer with a float value cant be directly added in doric
```
```scala mdoc:fail
concat("hi", 5) // expects only strings and a integer is found
```
