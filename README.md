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


Quick-Start
-----------
**TODO**


Plugin Directory Structure
--------------------------
Artifacts compiled from DSL model will be placed into SBT lib folder (`<project root>/lib`).  
The default plugin directory structure starts at `<project root>/model`:

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
