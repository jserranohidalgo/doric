# [doric](https://en.wikipedia.org/wiki/Doric_order)

Type-safe columns for DataFrames!

[![CI](https://github.com/hablapps/doric/actions/workflows/ci.yml/badge.svg)](https://github.com/hablapps/doric/actions/workflows/ci.yml)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/hablapps/doric/main?filepath=notebooks)
![Maven Central](https://img.shields.io/maven-central/v/org.hablapps/doric_2.12)
[![codecov](https://codecov.io/gh/hablapps/doric/branch/main/graph/badge.svg?token=N7ZXUXZX1I)](https://codecov.io/gh/hablapps/doric)

Doric offers type-safety in DataFrame column expressions at a minimum
cost, without compromising performance. In particular, doric allows you
to:

* Get rid of malformed column expressions at compile time
* Avoid implicit type castings
* Run DataFrames only when it is safe to do so
* Get all errors at once
* Modularize your business logic

You'll get all these goodies: 

* Without resorting to Datasets and sacrificing performance, i.e. sticking to DataFrames
* With minimal learning curve: almost no change in your code with respect to conventional column expressions
* Without fully committing to a strong static typing discipline throughout all your code

## User guide

Please, check out this [notebook](notebooks/README.ipynb) for examples
of use and rationale (also available through the
[binder](https://mybinder.org/v2/gh/hablapps/doric/HEAD?filepath=notebooks/README.ipynb)
link).

## Installation

Fetch the JAR from Maven:

```scala
libraryDependencies += "org.hablapps" %% "doric" % "0.0.1"
```

`Doric` depends on Spark internals, and it's been tested against the
following spark versions.

| Spark | Scala | doric  |
|-------|-------|-------|
| 3.1.0 | 2.12  | 0.0.1 |


## Contributing 

Doric is intended to offer a type-safe version of the whole Spark
Column API. Please, check the list of [open
issues](https://github.com/hablapps/doric/issues) and help us to
achieve that goal!
