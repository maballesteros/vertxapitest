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
  lazy val userRepo = new UserRepo(db)

  GET[EmptyParams, Array[User]]("/users") { params => userRepo.getAllAsync() }

  POST[EmptyParams, User, User]("/users") { (params, user) => userRepo.putAsync(user) }

  GET[UserIdParams, User]("/users/:userId") { params => userRepo.getAsync(params.userId) }

  DELETE[UserIdParams]("/users/:userId") { params => userRepo.deleteAsync(params.userId) }

  override def start {
    super.start
    userRepo.initAsync
  }
}

class UserRepo(val db: DatabaseAsync) extends DAO {

  def initAsync(): Future[Boolean] = async {
    await(db.executeAsync("DROP TABLE IF EXISTS test.users"))
    await(db.executeAsync("""
	  CREATE TABLE test.users (
	  id varchar(45) NOT NULL,
	  fname varchar(45) DEFAULT NULL,
	  lname varchar(45) DEFAULT NULL,
	  PRIMARY KEY (id)
	  ) ENGINE=InnoDB DEFAULT CHARSET=utf8
	  """))
    await(db.executeAsync("insert into test.users values ('1', 'Mike', 'Ballesteros Velasco')"))
    await(db.executeAsync("insert into test.users values ('2', 'Carlos', 'Ballesteros Velasco')"))
    await(db.executeAsync("insert into test.users values ('3', 'Jorge', 'Ballesteros García')"))
    await(db.executeAsync("insert into test.users values ('4', 'Marcos', 'Ballesteros García')"))
    println("Finalizada creación de BBDD")
    true
  }

  def getAllAsync(): Future[Array[User]] = async {
    val usersList = await(db.queryAsync[User](s"select * from test.users"))
    usersList.toArray
  }

  def getAsync(userId: String): Future[User] = async {
    val usersList = await(db.queryAsync[User](s"select * from test.users where id='$userId'"))
    usersList.head
  }

  def putAsync(user: User): Future[User] = async {
    val insertedRows = await(db.executeAsync(s"insert into users values ('${user.id}', '${user.fname}', '${user.lname}')"))
    user
  }

  def deleteAsync(userId: String): Future[Boolean] = async {
    val insertedRows = await(db.executeAsync(s"delete from test.users where id='$userId'"))
    insertedRows > 0
  }
}
