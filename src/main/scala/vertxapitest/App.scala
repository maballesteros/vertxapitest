package vertxapitest

import org.vertx.scala.platform.Verticle
import scala.util.Failure
import scala.util.Success


/**
 * Main app. Just init a Vertx app, providing the verticles to start
 */
object App {
  def main(args: Array[String]) {
    VertxApp.start(classOf[ApiServer])
  }
}

/**
 *  User case class and repo for memory storage
 */
case class User(id: String, fname: String, lname: String)

object UserRepo {

  var users = Map[String, User]()
  val mike = User("mike", "Miguel Ángel", "Ballesteros Velasco")
  users = users + (mike.id -> mike)

  def get(userId: String): User = users(userId)

  def put(user: User): Unit = {
    users = users + (user.id -> user)
  }
}

/**
 * Api server and API controller for user operations
 */
case class EmptyParams()
case class UserIdParams(userId: String)



class ApiServer extends Verticle {

  override def start() {

    import VertxRest._
    
    val db = new Db(this)
    
    val futConnection = db.connect("test", "pwd", 3306, "test")

    GET[UserIdParams, User]("/users/:userId")(
      (ur, ok) => {
        db.query[User](futConnection, "select * from test.users where id='"+ur.userId+"'").map{users =>
          ok(users.head)
        }
      }
    )

    POST[EmptyParams, User, User]("/users")(
      (er, user, ok) => {
        UserRepo.put(user)
        ok(user)
    })
    
    db.query[User](futConnection, "select * from test.users").map(users => println("USERS: " + users))
    
    println("Sent query!")

    val port = 9090
    startRestServer(vertx, port)
    println("Created server at " + port)
  }
}


