---
title: Doric Documentation
permalink: docs/quickstart/
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

```scala mdoc
import org.apache.spark.sql.{functions => f}
``` 

Next, make sure to activate the flag `spark.sql.datetime.java8API.enabled` when creating the Spark context.

```scala mdoc:invisible
val spark = _root_.doric.DocInit.getSpark
import spark.implicits._
```

The overall purpose of doric is providing a type-safe API on top of the DataFrame API. This essentially means 
that we aim at capturing errors at compile time. For instance, in Spark we can't mix apples and oranges, but this 
code still compiles:
```scala mdoc
def df = List(1,2,3).toDF.select($"value" * f.lit(true))
```
It's only when we try to construct the DataFrame that an exception is raised at _run-time_:
```scala mdoc:crash
df
``` 

Using doric, there is no need to wait for so long: errors will be reported at compile-time!
```scala mdoc:crash
List(1,2,3).toDF.select(col[Int]("value") * lit(true))
```

As you may see, changes in column expressions are minimal: just annotate column references with the intended type, 
i.e. `col[Int]("name")`, instead of a plain `col("name")`. 
---
**NOTE**

Of course, this only works if you, the programmer, know the intended type 
of the column at compile-time. In a pure dynamic setting, doric is useless. Note, however, that you don't need to know 
in advance the whole row type as with Datasets. Thus, doric sits between a wholehearted static setting and a 
purely dynamic one. It offers type-safety for column expressions at a minimum cost, without compromising performance, 
i.e. sticking to DataFrames.
---

Finally, once we have constructed a doric column expression, we can use it within the context of a `withColumn` expression, 
or, in general, wherever we may use plain Spark columns: joins, filters, etc.:

``` scala mdoc
List(1,2,3).toDF.withColumn("other", col[Int]("value") * lit(1))
```

As you can see in [validations](intro.md), the explicit type annotations enable further validations when columns
are interpreted with the context of `withColumn`, `select`, etc.

## Mixing doric and Spark columns

Since doric is intended as a replacement of the _whole_ DataFrame API, type-safe versions of Spark functions 
for numbers, dates, strings, etc., are provided. Occasionally, however, we might need to mix both doric and 
Spark column expressions. There is no problem with that, as this example shows: 

```scala mdoc
val strDf = List("hi", "welcome", "to", "doric").toDF("str")

strDf
  .select(f.concat(f.col("str"), f.lit("!!!")) as "newCol") //pure spark
  .select(concat(lit("???"), colString(c"newCol")) as c"finalCol") //pure and sweet doric
  .show()
```

Also, we can transform a pure Spark column into a doric column, and be sure that specific doric validations
will be applied:
```scala mdoc
strDf.select(f.col("str").asDoric[String]).show()
```

```scala mdoc:crash

strDf.select((f.col("str") + f.lit(true)).asDoric[String]).show
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

```scala mdoc
val column: DoricColumn[Int] = c"name"[Int]
``` 

Note also that column names can be implicitly converted to proper doric columns, provided that type information is
available from the context:
```scala mdoc
val column: DoricColumn[Int] = c"name"
```

### Dot syntax

Doric embraces the dot notation of common idiomatic Scala code wherever possible, instead of the SQL
_method-argument_ style of Spark SQL. For instance, given the following DataFrame: 
```scala mdoc
val dfArrays = List(("string", Array(1,2,3))).toDF("str", "arr")
```

a common transformation in the SQL style would go as follows:
```scala mdoc
import org.apache.spark.sql.Column

val sArrCol: Column = f.col("arr")
val sAddedOne: Column = f.transform(sArrCol, x => x + 1)
val sAddedAll: Column = f.aggregate(sAddedOne, f.lit(0), (x, y) => x + y)

dfArrays.select(sAddedAll as "complexTransformation").show
```

This complex transformation has to be developed in several steps, and usually tested step by step. 
The one-line version is not easy to read, either:

```scala mdoc
val complexS: Column = 
    f.aggregate(
        f.transform(f.col("arr"), x => x + 1), 
        f.lit(0), 
        (x, y) => x + y)

dfArrays.select(complexS as "complexTransformation").show
```

[//]: # (NOTE JM: why?)

Using doric, it's much easier to keep track of what we can do step-by-step:
```scala mdoc
val complexCol: DoricColumn[Int] = 
    col[Array[Int]](c"arr")
      .transform(_ + 1.lit)
      .aggregate(0.lit)(_ + _)
  
dfArrays.select(complexCol as c"complexTransformation").show
```

### Literal conversions

Sometimes, Spark allows us to insert literal values to simplify our code: 

```scala mdoc

val intDF = List(1,2,3).toDF("int")
val colS = f.col("int") + 1

intDF.select(colS).show
```

The default doric syntax is a little stricter and forces us to transform these values to literal columns:

```scala mdoc
val colD = colInt(c"int") + 1.lit

intDF.select(colD).show
```

In doric, we can also profit from the same literal syntax with the help of implicits. To enable this implicit behaviour,
however, we have to _explicitly_ add the following import statement: 

```scala mdoc
import _root_.doric.implicitConversions.literalConversion
val colSugarD = colInt(c"int") + 1
val columConcatLiterals = concat("this", "is","doric") // concat expects DoricColumn[String] values, the conversion puts them as expected

intDF.select(colSugarD, columConcatLiterals).show
```

Of course, implicit conversions are only in effect if the type of the literal value is valid:
```scala mdoc:fail
colInt("int") + 1f //an integer with a float value can't be directly added in doric
```
```scala mdoc:fail
concat("hi", 5) // expects only strings and an integer is found
```
