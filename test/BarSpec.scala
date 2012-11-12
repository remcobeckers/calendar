import models.{ Bars, Bar }

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.scalaquery.ql.extended.H2Driver.Implicit._
import org.scalaquery.session._
import org.scalaquery.session.Database.threadLocalSession

import play.api.test._
import play.api.test.Helpers._

class BarSpec extends FlatSpec with ShouldMatchers {

  "A Bar" should "be creatable" in {
    Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver") withSession {

      Bars.ddl.create
      Bars.insert(Bar(None, "foo"))
      val b = for (b <- Bars) yield b
      b.first.id.get should equal(1)
    }
  }
}