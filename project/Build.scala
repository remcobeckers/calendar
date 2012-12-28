import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Calendar"
    val appVersion      = "1.0"


  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "com.typesafe" % "slick_2.10" % "1.0.0-RC1",
    "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
	"postgresql" % "postgresql" % "9.1-901-1.jdbc4"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
