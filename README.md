SBT Console for ScalaIDE
========================

Adds a SBT Console inside the ScalaIDE for Eclipse.

A SBT Console can be launched using the Scala menu and choosing Show/hide SBT Console (default key shortcut: all three modifier keys + s).

If you have any problems or questions with usage, feel free to [submit tickets](https://github.com/SandroGrzicic/sbtconsole/issues/new) with questions, bug reports or feature requests.

Useful information can be found on the [project wiki](https://github.com/SandroGrzicic/sbtconsole/wiki).

Installing in Scala IDE:
=======================

Nightly update sites:

* Scala IDE 2.1.x, Scala 2.9: http://scala-ide.dreamhosters.com/nightly-update-sbtconsole-scalaide21-29/site/
* Scala IDE 2.1.x, Scala 2.10: http://scala-ide.dreamhosters.com/nightly-update-sbtconsole-scalaide21-210/site/

Scala IDE 2.1 is required; 2.0 is not supported.

Building:
=========

Use one of the following profiles, depending on the desired Scala version:

* `scala-ide-master-scala-2.9`
* `scala-ide-master-scala-trunk`

Run maven like this:

    mvn -P <scala-profile> clean install

After a successful build, you can find the local Eclipse update site in the folder `org.scala-ide.sdt.sbtconsole.update-site/target/site`. Make sure that the Scala IDE and Scala versions match.

If you update your local copy of the SBT Console sources and perform a new build, you can use the standard Eclipse update mechanism to update your locally built plugin.