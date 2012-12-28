package models

import play.api.Application
import scala.slick.driver.ExtendedProfile
import scala.slick.session.Database
import play.api.db.DB

trait DBAccess {
  val SLICK_DRIVER = "slick.db.driver"
  val DEFAULT_SLICK_DRIVER = "scala.slick.driver.H2Driver"

  def datamodel(implicit app: Application) = {
    val driverClass = app.configuration.getString(SLICK_DRIVER).getOrElse(DEFAULT_SLICK_DRIVER)
    val driver = singleton[ExtendedProfile](driverClass)
    new DataModel(driver)
  }

  def database(implicit app: Application) = {
    Database.forDataSource(DB.getDataSource())
  }

  private def singleton[T](name: String)(implicit man: Manifest[T]): T =
    Class.forName(name+"$").getField("MODULE$").get(man.runtimeClass).asInstanceOf[T]

}