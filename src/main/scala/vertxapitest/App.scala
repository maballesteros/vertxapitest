package vertxapitest

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future

import org.vertx.scala.core.VertxExecutionContext
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
 * Api server and API controller for user operations
 */
case class EmptyParams()
case class UserIdParams(userId: String)
case class User(id: String, fname: String, lname: String)

class ApiServer extends ApiVerticle(9090) {

  lazy val db = new DatabaseAsync(this, "mysql", 5, "test", "pwd", 3306, "test") // lazy it's important because verticle is not initialized at construction time
  lazy val userRepo = new UserRepo(db, this)

  GET[UserIdParams, User]("/users/:userId") { params => userRepo.getAsync(params.userId) }

  POST[EmptyParams, User, User]("/users") { (params, user) => userRepo.putAsync(user) }

}

class UserRepo(db: DatabaseAsync, verticle: Verticle) {
  implicit val context = VertxExecutionContext.fromVertxAccess(verticle)

  def getAsync(userId: String): Future[User] = async {
    val usersList = await(db.queryAsync[User]("select * from test.users where id='" + userId + "'"))
    usersList.head
  }

  def putAsync(user: User): Future[User] = async {
    val insertedRows = await(db.executeAsync(s"insert into users values ('${user.id}', '${user.fname}', '${user.lname}')"))
    user
  }
}
