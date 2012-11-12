package models

// Import the session management, including the implicit threadLocalSession
import org.scalaquery.session._
import org.scalaquery.session.Database.threadLocalSession

// Import the query language
import org.scalaquery.ql._

// Import the standard SQL types
import org.scalaquery.ql.TypeMapper._

// Use H2Driver which implements ExtendedProfile and thus requires ExtendedTables
import org.scalaquery.ql.extended.H2Driver.Implicit._
import org.scalaquery.ql.extended.{ ExtendedTable => Table }

case class Bar(id: Option[Int] = None, name: String)

object Bars extends Table[Bar]("bar") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  // This is the primary key column
  def name = column[String]("name")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = id.? ~ name <> (Bar, Bar.unapply _)
}