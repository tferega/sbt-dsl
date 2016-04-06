lazy val root = project in file(".") dependsOn(ProjectRef(file("../.."), "sbt-dsl"))
