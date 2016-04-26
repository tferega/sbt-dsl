SBT DSL Platform Plugin
=======================
Helps create and manage [DSL Platform](https://dsl-platform.com/) projects in Scala.


Installation
------------
_sbt-dsl_ requires _SBT_ `0.13.x` or greater.  
Add the following dependency to your `project/plugins.sbt`:

    addSbtPlugin("com.dslplatform" % "sbt-dsl" % "0.0.1-SNAPSHOT")

To add required dependencies and enable the plugin, add the following to your `build.sbt`:

    enablePlugins(SbtDslPlugin)
    libraryDependencies += "org.revenj" % "revenj-core" % "0.9.4"

_sbt-dsl_ also needs access to a _PostgreSQL_ database. If it is not at `localhost` on port `5432`, you will need to specify the HOST and PORT in `build.sbt`:

    dslDbLocation := Options.DbLocation("HOST", PORT)


Simple Example
--------------
To create an example _DSL project_ with default settings, just enter _dslExample_ command into the _SBT_ console, and provide password for `postgres` role on your DB when prompted:

    > dslInit

This will do the following:

  * Generate the directory structure and files needed for the _DSL Platform_
  * Create an example model for testing
  * Compile the model into a java library, to be used by client code
  * Create a DB role and database based on example model
  * Create an example Scala source file in `src/main/scala/Example.scala`

After initializing the project, make changes to the model (in `<project root>/model/dsl` folder) and use the _dslCompile_ command to recompile the java library and update the database:

    > dslCompile


Quick-Start
-----------
A minimum configuration for a _DSL project_ should include the following:

    dslDbLocation := Options.DbLocation("localhost", 5432)
    dslModule := "library"         // used when creating the database role and name
    dslNamespace := "com.example"  // generated code will be placed in this package

After configuration run _dslInit_ command to initialize the project:

    > dslInit

Now you can edit _DSL_ files in `model/dsl` folder. When ready to migrate the database and compile the files, run:

    > dslApply


Plugin Directory Structure
--------------------------
Artifacts compiled from DSL model will be placed into the _SBT_ unmanaged lib folder (`<project root>/lib`).  
Default directory structure of _sbt-dsl_ plugin starts at `<project root>/model`:

| Directory   | Description                                                      |
|-------------|------------------------------------------------------------------|
| `model/dsl` | Model definition in form of DSL files                            |
| `model/lib` | Libraries and dependencies needed to compile the model           |
| `model/sql` | SQL files generated from the model used to generate the database |


Usage
-----
**TODO** main task list


Configuration
-------------
**TODO** main settings list
