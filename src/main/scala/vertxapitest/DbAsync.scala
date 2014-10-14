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

/**
 * Helper class to do async DB access and mapping to case clases
 */
class DatabaseAsync(verticle: Verticle) {

  val executionContext = VertxExecutionContext.fromVertxAccess(verticle)

  def connect(username: String, password: String, port: Int, database: String): Future[Connection] = {
    try {
      new MySQLConnection(
        Configuration(username = username, port = port, password = Option(password), database = Option(database)),
        group = verticle.vertx.asJava.currentContext().asInstanceOf[EventLoopContext].getEventLoop(),
        executionContext = executionContext).connect
    } catch {
      case t: Throwable => t.printStackTrace(); null
    }
  }

  import scala.concurrent._

  def queryAsync[T](futConnection: Future[Connection], sql: String)(implicit t: Manifest[T]): Future[List[T]] = {
    implicit val context = executionContext
    try {
      val out = for {
        con <- futConnection
        queryRes <- con.sendQuery(sql)
      } yield {
        val rs = queryRes.rows.get

        try {
          var data: List[T] = rs.map(rd => {
            ReflectionUtils.instantiate[T](f => rd(f.getName).asInstanceOf[AnyRef]).asInstanceOf[T]
          }).toList
          data
        } catch {
          case t: Throwable => {
            t.printStackTrace()
            Nil
          }
        }
      }
      out
    } catch {
      case t => t.printStackTrace(); future(List())
    }
  }
}
