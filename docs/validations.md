---
title: Doric Documentation
permalink: docs/validations/
---


# Doric validations

Let's suppose that you make a reference to a non-existing column. No problem, Spark will detect 
that and will complain with an exception at runtime:

```scala
List(1,2,3).toDF.select(f.col("id")+1)
// org.apache.spark.sql.AnalysisException: cannot resolve '`id`' given input columns: [value];
// 'Project [('id + 1) AS (id + 1)#499]
// +- LocalRelation [value#495]
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

Now, let's assume that the column exists but its type is not what we expected. Spark won't be able to detect that, 
since type expectations are not encoded in plain columns. Thus, the following code will compile and execute 
without errors:

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
//   	located at . (validations.md:49)
// 
// 	at doric.sem.package$ErrorThrower.$anonfun$returnOrThrow$1(package.scala:9)
// 	at cats.data.Validated.fold(Validated.scala:29)
// 	at doric.sem.package$ErrorThrower.returnOrThrow(package.scala:9)
// 	at doric.sem.TransformOps$DataframeTransformationSyntax.select(TransformOps.scala:139)
// 	at repl.MdocSession$App$$anonfun$6.apply$mcV$sp(validations.md:49)
// 	at repl.MdocSession$App$$anonfun$6.apply(validations.md:48)
// 	at repl.MdocSession$App$$anonfun$6.apply(validations.md:48)
```

Moreover, note that the location of the error is also included. This will prove immensely useful, as we will see 
[later on](error-location.md)!

