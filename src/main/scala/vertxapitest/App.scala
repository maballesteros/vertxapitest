package vertxapitest

import org.vertx.scala.platform.Verticle
import scala.util.Failure
import scala.util.Success
import scala.concurrent.Future
import org.vertx.scala.core.VertxExecutionContext
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.async.Async.{ async, await }

/**
 * Main app. Just init a Vertx app, providing the verticles to start
 */
object App {
  def main(args: Array[String]) {
    VertxApp.start(classOf[ApiServer])
  }
}

/**
 * Api server and API controller for user operations
 */
case class EmptyParams()
case class UserIdParams(userId: String)

class ApiServer extends ApiVerticle(9090) {

  lazy val db = new DatabaseAsync(this)  // lazy it's important because
  lazy val userRepo = new UserRepo(db)

  GET[UserIdParams, User]("/users/:userId") { params => userRepo.getAsync(params.userId) }

  POST[EmptyParams, User, User]("/users") { (params, user) => userRepo.putAsync(user) }

}

/**
 *  User case class and repo for memory storage
 */
case class User(id: String, fname: String, lname: String)

class UserRepo(db: DatabaseAsync) {

  val futConnection = db.connect("test", "pwd", 3306, "test")

  def getAsync(userId: String): Future[User] = async {
    val usersList = await(db.queryAsync[User](futConnection, "select * from test.users where id='" + userId + "'"))
    usersList.head
  }

  def putAsync(user: User): Future[User] = {
    future(user)
  }
}
