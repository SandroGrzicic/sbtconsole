SBT Console for ScalaIDE
========================

Adds a SBT Console inside the ScalaIDE for Eclipse.

A SBT Console can be launched using the Scala menu and choosing Show/hide SBT Console (default key shortcut CTRL+ALT+SHIFT+S).

The project is still in heavy development, so feel free to submit tickets with bug reports and feature requests.


Installing in ScalaIDE:
=======================

Eclipse installation should be performed using the ScalaIDE Ecosystem. Choose one of these update sites, according to which ScalaIDE version you already have:

* http://download.scala-ide.org/ecosystem/dev-2.0-2.9/site/

* http://download.scala-ide.org/ecosystem/dev-master-2.9/site/


Building:
=========

By default, the maven build is performed against the latest stable versions (Scala IDE 2.0 and Scala 2.9).
The available profiles are:

* `scala-ide-2.0-scala-2.9` (default)
* `scala-ide-2.0.x-scala-2.9`
* `scala-ide-master-scala-2.9`
* `scala-ide-master-scala-trunk`

Run maven like this:

    mvn -P scala-ide-master-scala-trunk clean install

