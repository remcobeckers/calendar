package models

import scala.slick.driver.ExtendedProfile

class DataModel(val profile: ExtendedProfile) extends Profile with UserComponent with EventsComponent {

}