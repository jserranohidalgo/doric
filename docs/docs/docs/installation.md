---
title: Install doric in your project
permalink: docs/installation/
---
Not yet sorry
# Installing doric
Doric is compatible with spark version @SPARK_VERSION@. Just add the dependency in your build tool.

The latest stable version of doric is @STABLE_VERSION@.

The latest experimental version of doric is @VERSION@.

## Sbt
```scala
libraryDependencies += "org.hablapps" % "doric_2.12" % "@STABLE_VERSION@"
```
## Maven
```xml
<dependency>
  <groupId>org.hablapps</groupId>
  <artifactId>doric_2.12</artifactId>
  <version>@STABLE_VERSION@</version>
</dependency>
```

Doric requires to activate the following flag when creating the spark context:
`spark.sql.datetime.java8API.enabled` equal to true.

Doric is committed to use the most modern API's first.
