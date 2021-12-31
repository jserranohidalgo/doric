---
title: Doric Documentation
permalink: docs/validations/
---


# Doric validations

Doric is a type-safe API, which means that many common errors will be captured at compile-time. However, there
are errors which can't be anticipated, since they depend on the actual datasource available at runtime.
For instance, we might make reference to a non-existing column. In that case, doric behaves similarly to Spark,
raising a run-time exception: 

```scala
// Spark
List(1,2,3).toDF.select(f.col("id")+1)
// org.apache.spark.sql.AnalysisException: cannot resolve '`id`' given input columns: [value];
// 'Project [('id + 1) AS (id + 1)#599]
// +- LocalRelation [value#595]
// 
// 	at org.apache.spark.sql.catalyst.analysis.package$AnalysisErrorAt.failAnalysis(package.scala:42)
// 	at org.apache.spark.sql.catalyst.analysis.CheckAnalysis$$anonfun$$nestedInanonfun$checkAnalysis$1$2.applyOrElse(CheckAnalysis.scala:155)
// 	at org.apache.spark.sql.catalyst.analysis.CheckAnalysis$$anonfun$$nestedInanonfun$checkAnalysis$1$2.applyOrElse(CheckAnalysis.scala:152)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$transformUp$2(TreeNode.scala:342)
// 	at org.apache.spark.sql.catalyst.trees.CurrentOrigin$.withOrigin(TreeNode.scala:74)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.transformUp(TreeNode.scala:342)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$transformUp$1(TreeNode.scala:339)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.$anonfun$mapChildren$1(TreeNode.scala:408)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.mapProductIterator(TreeNode.scala:244)
// 	at org.apache.spark.sql.catalyst.trees.TreeNode.mapChildren(TreeNode.scala:406)
```

```scala
// Doric
List(1,2,3).toDF.select(colInt("id")+1)
// doric.sem.DoricMultiError: Found 1 error in select
//   Cannot resolve column name "id" among (value)
//   	located at . (validations.md:37)
// 
// 	at doric.sem.package$ErrorThrower.$anonfun$returnOrThrow$1(package.scala:9)
// 	at cats.data.Validated.fold(Validated.scala:29)
// 	at doric.sem.package$ErrorThrower.returnOrThrow(package.scala:9)
// 	at doric.sem.TransformOps$DataframeTransformationSyntax.select(TransformOps.scala:139)
// 	at repl.MdocSession$App$$anonfun$4.apply(validations.md:37)
// 	at repl.MdocSession$App$$anonfun$4.apply(validations.md:37)
// Caused by: org.apache.spark.sql.AnalysisException: Cannot resolve column name "id" among (value)
// 	at org.apache.spark.sql.Dataset.org$apache$spark$sql$Dataset$$resolveException(Dataset.scala:272)
// 	at org.apache.spark.sql.Dataset.$anonfun$resolve$1(Dataset.scala:263)
// 	at scala.Option.getOrElse(Option.scala:189)
// 	at org.apache.spark.sql.Dataset.resolve(Dataset.scala:263)
// 	at org.apache.spark.sql.Dataset.col(Dataset.scala:1359)
// 	at org.apache.spark.sql.Dataset.apply(Dataset.scala:1326)
// 	at doric.types.SparkType.$anonfun$validate$1(SparkType.scala:56)
// 	at cats.data.KleisliApply.$anonfun$product$2(Kleisli.scala:674)
// 	at cats.data.Kleisli.$anonfun$map$1(Kleisli.scala:40)
// 	at cats.data.Kleisli.$anonfun$map$1(Kleisli.scala:40)
```

However, there is a slight difference in the exception reported: doric adds precise information about the location
of the error in the source code, which in many cases is immensely useful (e.g. to support the development of [reusable
functions](modularity.md)). 

## Mismatch types

But doric goes further, thanks to the type annotations that it supports. Indeed, let's assume that the column 
exists but its type is not what we expected: Spark won't be able to detect that, since type expectations are not 
encoded in plain columns. Thus, the following code will compile and execute without errors:

```scala
val df = List("1","2","three").toDF.select(f.col("value") + 1)
// df: org.apache.spark.sql.package.DataFrame = [(value + 1): double]
```

and we will be able to run the DataFrame:

```scala
df.show
// +-----------+
// |(value + 1)|
// +-----------+
// |        2.0|
// |        3.0|
// |       null|
// +-----------+
//
```

obtaining null values and garbage results, in general.

Using doric we can prevent the creation of the DataFrame, since column expressions are typed:

```scala
val df = List("1","2","three").toDF.select(colInt("value") + 1.lit)
// doric.sem.DoricMultiError: Found 1 error in select
//   The column with name 'value' is of type StringType and it was expected to be IntegerType
//   	located at . (validations.md:59)
// 
// 	at doric.sem.package$ErrorThrower.$anonfun$returnOrThrow$1(package.scala:9)
// 	at cats.data.Validated.fold(Validated.scala:29)
// 	at doric.sem.package$ErrorThrower.returnOrThrow(package.scala:9)
// 	at doric.sem.TransformOps$DataframeTransformationSyntax.select(TransformOps.scala:139)
// 	at repl.MdocSession$App$$anonfun$8.apply$mcV$sp(validations.md:59)
// 	at repl.MdocSession$App$$anonfun$8.apply(validations.md:58)
// 	at repl.MdocSession$App$$anonfun$8.apply(validations.md:58)
```

## Error aggregation

Doric departs from Spark in an additional aspect of error management: Sparks adopts a fail-fast strategy, in such 
a way that it will stop at the first error encountered, whereas doric will keep accumulating errors throughout the
whole column expression. This is essential to speed up and facilitate the solution to most common development problems.

For instance, let's consider the following code where we encounter three erroneous column references:

```scala
val dfPair = List(("hi", 31)).toDF("str", "int")
val col1 = colInt("str")   // existing column, wrong type
val col2 = colString("int") // existing column, wrong type
val col3 = colInt("unknown") // non-existing column
```

```scala
dfPair.select(col1, col2, col3)
// doric.sem.DoricMultiError: Found 3 errors in select
//   The column with name 'str' is of type StringType and it was expected to be IntegerType
//   	located at . (validations.md:71)
//   The column with name 'int' is of type IntegerType and it was expected to be StringType
//   	located at . (validations.md:74)
//   Cannot resolve column name "unknown" among (str, int)
//   	located at . (validations.md:77)
// 
// 	at doric.sem.package$ErrorThrower.$anonfun$returnOrThrow$1(package.scala:9)
// 	at cats.data.Validated.fold(Validated.scala:29)
// 	at doric.sem.package$ErrorThrower.returnOrThrow(package.scala:9)
// 	at doric.sem.TransformOps$DataframeTransformationSyntax.select(TransformOps.scala:139)
// 	at repl.MdocSession$App$$anonfun$14.apply(validations.md:84)
// 	at repl.MdocSession$App$$anonfun$14.apply(validations.md:84)
```

As we can see, the select expression throws a _single_ exception reporting the three different errors. There is no
need to start an annoying fix-rerun loop until all errors are found. Moreover, note that each error points to the 
line of the corresponding column expression where it took place. 

