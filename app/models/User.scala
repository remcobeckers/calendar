package models

// Import the session management, including the implicit threadLocalSession
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar

case class User(id: Option[Int] = None, openId: String, email: String, firstname: String, lastname: String)

trait UserComponent {
  this: Profile =>

  import profile.simple._
  import Database.threadLocalSession

  object Users extends Table[User]("user") {

    // This is the primary key column
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def openId = column[String]("openId", O.NotNull)
    def email = column[String]("email")
    def firstname = column[String]("firstname")
    def lastname = column[String]("lastname")
    def uniqueUserId = index("unique_user_id_idx", openId, unique = true)

    // Every table needs a * projection with the same type as the table's type parameter
    def * = id.? ~ openId ~ email ~ firstname ~ lastname <> (User, User.unapply _)

    def findOrCreate(oId: String)(modify: User => User) = {
      val user = find(oId)
      user firstOption match {
        case Some(u) => user.update(modify(u))
        case None => {
          val u = modify(User(None, oId, "", "", ""))
          openId ~ email ~ firstname ~ lastname insert (u.openId, u.email, u.firstname, u.lastname)
        }
      }
      find(oId) first
    }

    def find(oId: String) = for (u <- Users if u.openId === oId) yield u
  }

}
