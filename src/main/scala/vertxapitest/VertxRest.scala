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

object VertxApp {

  def start(serverClass: Class[_]) = {
    val urlClassLoader: URLClassLoader = (serverClass.getClassLoader).asInstanceOf[URLClassLoader]
    PlatformLocator.factory.createPlatformManager().deployVerticle(serverClass.getName(), new JsonObject(), urlClassLoader.getURLs(), 1, null, null)
    new Semaphore(0).acquire()
  }

}

object VertxRest {

  val routeMatcher = RouteMatcher()

  private val gson = new Gson()

  def startRestServer(vertx: Vertx, port: Int) = {
    vertx.createHttpServer()
      .setCompressionSupported(true)
      .requestHandler(routeMatcher)
      .listen(port, "localhost")
  }

  def GET[P, R](pattern: String)(func: (P, (R => Unit)) => Unit)(implicit t: Manifest[P]): Unit = {
    routeMatcher.get(pattern, (req: HttpServerRequest) => {
      func(parseRequestParams[P](req).asInstanceOf[P], (res: R) => req.response.putHeader("Content-type", "application/json").end(toJson(res)))
    })
  }

  def POST[P, R](pattern: String)(func: (P, (R => Unit)) => Unit)(implicit t: Manifest[P]): Unit = {
    routeMatcher.post(pattern, (req: HttpServerRequest) => {
      req.bodyHandler({ body: Buffer =>
        func(parseRequestParams[P](req).asInstanceOf[P],
          (res: R) => req.response.putHeader("Content-type", "application/json").end(toJson(res)))
      })
    })
  }

  def POST[P, B, R](pattern: String)(func: (P, B, (R => Unit)) => Unit)(implicit p: Manifest[P], b: Manifest[B]): Unit = {
    routeMatcher.post(pattern, (req: HttpServerRequest) => {
      req.bodyHandler({ body: Buffer =>
        func(parseRequestParams[P](req).asInstanceOf[P], fromJson(body.toString, b.erasure).asInstanceOf[B],
          (res: R) => req.response.putHeader("Content-type", "application/json").end(toJson(res)))
      })
    })
  }

  private def toJson(any: Any): String = gson.toJson(any)

  private def fromJson[T](json: String, clazz: Class[T]): T = gson.fromJson(json, clazz)

  private def responseJson(req: HttpServerRequest, any: Any) = req.response().end(toJson(any))

  private def parseRequestParams[T](req: HttpServerRequest)(implicit t: Manifest[T]): Any = {
    Instantiator.instantiate[T](f => req.params.get(f.getName).get.headOption.get)
  }
}

object Instantiator {

  def instantiate[T](f: Field => String)(implicit t: Manifest[T]) = {
    val consArgs = t.erasure.getDeclaredFields().filter(f => f.getName().charAt(0) != '$').map(f)
    val cons = t.erasure.getConstructors()
    //println("Calling constructor", cons(0) ," with: ", consArgs.toList)
    val instance = cons(0).newInstance(consArgs.toArray: _*)
    instance
  }
}

class Db(verticle: Verticle) {

  val executionContext = VertxExecutionContext.fromVertxAccess(verticle)

  def connect(username: String, password: String, port: Int, database: String): Future[Connection] = {
    new MySQLConnection(
      Configuration(username = username, port = port, password = Option(password), database = Option(database)),
      group = verticle.vertx.asJava.currentContext().asInstanceOf[EventLoopContext].getEventLoop(),
      executionContext = executionContext).connect
  }

  import scala.concurrent._

  def query[T](futConnection: Future[Connection], sql: String)(implicit t: Manifest[T]): Future[List[T]] = {
    implicit val context = executionContext
    val out = for {
      con <- futConnection
      queryRes <- con.sendQuery(sql)
    } yield {
      val rs = queryRes.rows.get

      try {
        var data: List[T] = rs.map(rd => {
          Instantiator.instantiate[T](f => rd(f.getName) match { case null => null; case x => x.toString }).asInstanceOf[T]
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
  }
}
