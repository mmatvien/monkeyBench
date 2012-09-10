import sbt._
import Keys._

import PlayProject._

object ApplicationBuild extends Build {

  val appName = "pet"
  val appVersion = "1.0"

  val appDependencies = Seq(
    "se.radley" %% "play-plugins-salat" % "1.0.9",
    "commons-codec" % "commons-codec" % "1.5"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers ++= Seq("repo.novus snaps" at "http://repo.novus.com/snapshots/"),

    routesImport += "se.radley.plugin.salat.Binders._",
    templatesImport += "org.bson.types.ObjectId"
  )
}
            
