SBT DSL Platform Plugin
=======================
Helps create and manage [DSL Platform](https://dsl-platform.com/) projects in Scala.


Installation
------------
_sbt-dsl_ requires SBT 0.13.x or greater.  
Add the following dependency to your `project/plugins.sbt`:

    addSbtPlugin("com.dslplatform" % "sbt-dsl" % "0.0.1-SNAPSHOT")

To import the default settings and enable the plugin, add the following to your `build.sbt`:

    enablePlugins(SbtDslPlugin)

_sbt-dsl_ also needs access to _PostgreSQL_ database.


Quick-Start
-----------
After installation, if your database is not at `localhost` on port `5432`, you will need to specify the HOST and PORT in `build.sbt`:

    dslDbLocation := Options.DbLocation("HOST", PORT)

That's it. To start using _sbt-dsl_, just enter _dslInit_ command into your _SBT_ console, and provide password for `postgres` role on your DB when prompted:

    > dslInit

This will do the following:

  * Generate the directory structure and files needed for the plugin
  * Create an example model for testing
  * Compile the model into a java library
  * Create a DB role and database based on example model

After making changed to the model use the _dslCompile_ command to recompile the java library and update the database:

    > dslCompile


Plugin Directory Structure
--------------------------
Artifacts compiled from DSL model will be placed into the _SBT_ lib folder (`<project root>/lib`).  
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
