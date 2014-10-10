package vertxapitest

import org.vertx.scala.platform.Verticle


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

    GET[UserIdParams, User]("/users/:userId")(
      (ur, ok) => ok(UserRepo.get(ur.userId))
    )

    POST[EmptyParams, User, User]("/users")(
      (er, user, ok) => {
        UserRepo.put(user)
        ok(user)
    })

    val port = 8080
    startRestServer(vertx, port)
    println("Created server at " + port)
  }
}


