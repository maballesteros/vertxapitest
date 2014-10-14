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
 * Helper object to start Vertx
 */
object VertxApp {

  def start(serverClass: Class[_]) = {
    val urlClassLoader: URLClassLoader = (serverClass.getClassLoader).asInstanceOf[URLClassLoader]
    PlatformLocator.factory.createPlatformManager().deployVerticle(serverClass.getName(), new JsonObject(), urlClassLoader.getURLs(), 1, null, null)
    new Semaphore(0).acquire()
  }

}

/**
 * Absctract API verticle that provided convenience methods for REST API publishing
 */
abstract class ApiVerticle(port: Int) extends Verticle {

  implicit val context = VertxExecutionContext.fromVertxAccess(this)
  private val routeMatcher = RouteMatcher()
  private val gson = new Gson()

  override def start() {
    vertx.createHttpServer()
      .setCompressionSupported(true)
      .requestHandler(routeMatcher)
      .listen(port, "0.0.0.0")
    println("Created Api at " + port)
  }

  def GET[P, R](pattern: String)(func: P => Future[R])(implicit t: Manifest[P]): Unit = {
    routeMatcher.get(pattern, (req: HttpServerRequest) => {
      func(parseRequestParams[P](req).asInstanceOf[P]).map(res => {
        req.response.putHeader("Content-type", "application/json").end(toJson(res))
      })(context)
    })
  }

  def DELETE[P](pattern: String)(func: P => Future[Boolean])(implicit t: Manifest[P]): Unit = {
    routeMatcher.delete(pattern, (req: HttpServerRequest) => {
      func(parseRequestParams[P](req).asInstanceOf[P]).map(res => {
        req.response.end()
      })(context)
    })
  }

  def POST[P, R](pattern: String)(func: P => Future[R])(implicit t: Manifest[P]): Unit = {
    routeMatcher.post(pattern, (req: HttpServerRequest) => {
      req.bodyHandler({ body: Buffer =>
        func(parseRequestParams[P](req).asInstanceOf[P]).map(res => {
          req.response.putHeader("Content-type", "application/json").end(toJson(res))
        })(context)
      })
    })
  }

  def POST[P, B, R](pattern: String)(func: (P, B) => Future[R])(implicit p: Manifest[P], b: Manifest[B]): Unit = {
    routeMatcher.post(pattern, (req: HttpServerRequest) => {
      req.bodyHandler({ body: Buffer =>
        func(parseRequestParams[P](req).asInstanceOf[P], fromJson(body.toString, b.erasure).asInstanceOf[B]).map(res => {
          req.response.putHeader("Content-type", "application/json").end(toJson(res))
        })(context)
      })
    })
  }

  private def toJson(any: Any): String = gson.toJson(any)

  private def fromJson[T](json: String, clazz: Class[T]): T = gson.fromJson(json, clazz)

  private def responseJson(req: HttpServerRequest, any: Any) = req.response().end(toJson(any))

  private def parseRequestParams[T](req: HttpServerRequest)(implicit t: Manifest[T]): Any = {
    ReflectionUtils.instantiate[T](f => req.params.get(f.getName).get.headOption.get)
  }
}

