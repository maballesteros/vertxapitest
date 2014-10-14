package vertxapitest

import java.lang.reflect.Field
import java.net.URLClassLoader
import java.util.concurrent.Semaphore
import scala.collection.mutable.MultiMap
import org.vertx.java.core.http.{ HttpServerRequest => JHttpServerRequest }
import org.vertx.java.core.http.{ RouteMatcher => JRouteMatcher }
import org.vertx.java.core.impl.EventLoopContext
import org.vertx.java.core.json.JsonObject
import org.vertx.java.platform.PlatformLocator
import org.vertx.scala.core.Vertx
import org.vertx.scala.core.VertxExecutionContext
import org.vertx.scala.core.buffer.Buffer
import org.vertx.scala.core.http.HttpServerRequest
import org.vertx.scala.core.http.RouteMatcher
import org.vertx.scala.platform.Verticle
import com.github.mauricio.async.db._
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.google.gson.Gson
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.async.Async.{ async, await }

/**
 * Helper class to do async DB access and mapping to case clases
 */
class DatabaseAsync(val verticle: Verticle, databaseType:String, poolSize: Int, username: String, password: String, port: Int, database: String) {

  import scala.concurrent._

  val executionContext = VertxExecutionContext.fromVertxAccess(verticle)

  val configuration = Configuration(username = username, port = port, password = Option(password), database = Option(database))

  val pool = db.AsyncConnectionPool(verticle, databaseType, poolSize, configuration)

  def queryAsync[T](sql: String)(implicit t: Manifest[T]): Future[List[T]] =
    pool.withConnection(con => {
      async {
        val queryResult = await(con.sendQuery(sql))
        queryResult.rows.get.map(rd => {
          try {
        	  ReflectionUtils.instantiate[T](f => rd(f.getName).asInstanceOf[AnyRef]).asInstanceOf[T]
          } catch {
            case t => t.printStackTrace(); None.asInstanceOf[T]
          }
        }).toList
      }(executionContext)
    })

  def executeAsync(sql: String): Future[Long] =
    pool.withConnection(con => {
      async {
        val queryResult = await(con.sendQuery(sql))
        queryResult.rowsAffected
      }(executionContext)
    })

}


trait DAO {
  val db: DatabaseAsync
  implicit val context = VertxExecutionContext.fromVertxAccess(db.verticle)
}
