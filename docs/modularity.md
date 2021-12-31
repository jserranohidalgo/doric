---
title: Doric Documentation
permalink: docs/modularity/
---


# Fostering modularity

Doric main focus is to give the possibility of modularize the logic for the etl, and the idea to find fast the location of an error is essential.
Let's make an example, imagine that we have some columns have a suffix in the name telling us is information of a user.


```scala
userDF.show
// +---------+---------+--------+
// |name_user|city_user|age_user|
// +---------+---------+--------+
// |      Foo|   Madrid|      35|
// |      Bar| New York|      40|
// |     John|    Paris|      30|
// +---------+---------+--------+
// 
userDF.printSchema
// root
//  |-- name_user: string (nullable = true)
//  |-- city_user: string (nullable = true)
//  |-- age_user: integer (nullable = false)
//
```

Us as developers want to abstract from this suffix and focus only in the unique part of the name:
```scala
colString(c"name_user")
// res2: NamedDoricColumn[String] = NamedDoricColumn(
//   Kleisli(doric.types.SparkType$$Lambda$1489/1909117445@332abea0),
//   "name_user"
// )
colInt(c"age_user")
// res3: NamedDoricColumn[Int] = NamedDoricColumn(
//   Kleisli(doric.types.SparkType$$Lambda$1489/1909117445@125c6004),
//   "age_user"
// )
colString(c"city_user")
// res4: NamedDoricColumn[String] = NamedDoricColumn(
//   Kleisli(doric.types.SparkType$$Lambda$1489/1909117445@4c59f5e8),
//   "city_user"
// )
```
So we can make a function to simplify it:
```scala
import doric.types.SparkType
def user[T: SparkType](colName: CName): DoricColumn[T] = {
  col[T](colName + "_user")
}
```
In valid cases it works ok, bug when an error is produce because one of these references, it will point to the line `col[T](colName + "_user")` that is not the real problem.

```scala
val userc = user[Int](c"name") //wrong type :S
userDF.select(userc)
// doric.sem.DoricMultiError: Found 1 error in select
//   The column with name 'name_user' is of type StringType and it was expected to be IntegerType
//   	located at . (modularity.md:61)
// 
// 	at doric.sem.package$ErrorThrower.$anonfun$returnOrThrow$1(package.scala:9)
// 	at cats.data.Validated.fold(Validated.scala:29)
// 	at doric.sem.package$ErrorThrower.returnOrThrow(package.scala:9)
// 	at doric.sem.TransformOps$DataframeTransformationSyntax.select(TransformOps.scala:139)
// 	at repl.MdocSession$App$$anonfun$8.apply(modularity.md:70)
// 	at repl.MdocSession$App$$anonfun$8.apply(modularity.md:68)
```

What we really want is to mark as the source the place we are using our `user` method. We can achieve this by adding only an implicit value to the definition:

```scala
import doric._
import doric.sem.Location
import doric.types.SparkType

def user[T: SparkType](colName: CName)(implicit location: Location): DoricColumn[T] = {
  col[T](colName + "_user")
}
```
Now if we repeat the same error we will be pointed to the real problem
```scala
val age = user[Int](c"name")
val team = user[String](c"team")
userDF.select(age, team)
// doric.sem.DoricMultiError: Found 2 errors in select
//   The column with name 'name_user' is of type StringType and it was expected to be IntegerType
//   	located at . (modularity.md:116)
//   Cannot resolve column name "team_user" among (name_user, city_user, age_user)
//   	located at . (modularity.md:117)
// 
// 	at doric.sem.package$ErrorThrower.$anonfun$returnOrThrow$1(package.scala:9)
// 	at cats.data.Validated.fold(Validated.scala:29)
// 	at doric.sem.package$ErrorThrower.returnOrThrow(package.scala:9)
// 	at doric.sem.TransformOps$DataframeTransformationSyntax.select(TransformOps.scala:139)
// 	at repl.MdocSession$App5$$anonfun$12.apply(modularity.md:118)
// 	at repl.MdocSession$App5$$anonfun$12.apply(modularity.md:115)
```

