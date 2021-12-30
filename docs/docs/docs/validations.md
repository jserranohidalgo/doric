---
title: Doric Documentation
permalink: docs/validations/
---

```scala mdoc:invisible
import org.apache.spark.sql.{functions => f}

val spark = doric.DocInit.getSpark
import spark.implicits._

import doric._
import doric.implicitConversions.stringCname
```

# Doric validations

Let's suppose that you make a reference to a non-existing column. No problem, Spark will detect 
that and will complain with an exception at runtime:

```scala mdoc:crash
List(1,2,3).toDF.select(f.col("id")+1)
```

Now, let's assume that the column exists but its type is not what we expected. Spark won't be able to detect that, 
since type expectations are not encoded in plain columns. Thus, the following code will compile and execute 
without errors:

```scala mdoc
val df = List("1","2","three").toDF.select(f.col("value") + 1)
```

and we will be able to run the DataFrame:

```scala mdoc
df.show
```

obtaining null values and garbage results, in general.

Using doric we can prevent the creation of the DataFrame, since column expressions are typed:

```scala mdoc:crash
val df = List("1","2","three").toDF.select(colInt("value") + 1.lit)
```

Moreover, note that the location of the error is also included. This will prove immensely useful, as we will see 
[later on](error-location.md)!

