---
title: Doric Documentation
permalink: docs/adt-columns/
---
# Doric column ADT hierarchy
Doric column is an algebraic data type (ADT) that represent the validations and transformations that are needed to obtain the spark expression required.
Doric column has two subtypes of column, that can provide more info about the content of the column.

## TransformationDoricColumn
It's the most basic type of column, that only carries with the needed info about transformations.

## NamedDoricColumn
A column with a defined name. You can obtain this kind of columns in two ways, referring a dataframe column, with `col[String](c"colname")` for example, or assigning an alias/as to a transformed column `mycolumn.as(c"mycol")`.
This way Doric helps you to always carrie the name that was assigned and allows you to extract it.

```scala
import doric._

val colFromDF: NamedDoricColumn[String] = col[String]("user")
// colFromDF: NamedDoricColumn[String] = NamedDoricColumn(
//   Kleisli(doric.types.SparkType$$Lambda$1433/1503391960@492be039),
//   "user"
// )
val colWithAlias: NamedDoricColumn[Int] = col[Int]("int1") + col[Int]("int2") as "newVal"
// colWithAlias: NamedDoricColumn[Int] = NamedDoricColumn(
//   Kleisli(cats.data.Kleisli$$Lambda$1437/1647656571@7b96de8d),
//   "newVal"
// )

println(colFromDF.columnName)
// user
println(colWithAlias.columnName)
// newVal
```
