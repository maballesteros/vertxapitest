package vertxapitest

import org.vertx.java.core.http.{ HttpServerRequest => JHttpServerRequest, RouteMatcher => JRouteMatcher }
import org.vertx.scala.core.http.RouteMatcher
import org.vertx.scala.core.http.HttpServerRequest
import java.util.concurrent.Semaphore
import org.vertx.java.platform.PlatformLocator
import java.net.URLClassLoader
import org.vertx.java.core.json.JsonObject
import com.google.gson.Gson
import java.lang.reflect.Field
import scala.collection.mutable.MultiMap
import org.vertx.scala.core.buffer.Buffer
import org.vertx.scala.core.Vertx

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
  
  def startRestServer(vertx:Vertx, port: Int) = {
    vertx.createHttpServer()
      .setCompressionSupported(true)
      .requestHandler(routeMatcher)
      .listen(port, "localhost")
  }
 
  def GET[P, R](pattern: String)(func: (P, (R => Unit)) => Unit)(implicit t: Manifest[P]): Unit = {
    routeMatcher.get(pattern, (req: HttpServerRequest) => {
      func(parseRequestParams(req, t.erasure).asInstanceOf[P], (res: R) => req.response.putHeader("Content-type", "application/json").end(toJson(res)))
    })
  }

  def POST[P, R](pattern: String)(func: (P, (R => Unit)) => Unit)(implicit t: Manifest[P]): Unit = {
    routeMatcher.post(pattern, (req: HttpServerRequest) => {
      req.bodyHandler({ body: Buffer =>
        func(parseRequestParams(req, t.erasure).asInstanceOf[P], 
            (res: R) => req.response.putHeader("Content-type", "application/json").end(toJson(res)))
      })
    })
  }

  def POST[P, B, R](pattern: String)(func: (P, B, (R => Unit)) => Unit)(implicit p: Manifest[P], b: Manifest[B]): Unit = {
    routeMatcher.post(pattern, (req: HttpServerRequest) => {
      req.bodyHandler({ body: Buffer =>
        func(parseRequestParams(req, p.erasure).asInstanceOf[P], fromJson(body.toString, b.erasure).asInstanceOf[B], 
            (res: R) => req.response.putHeader("Content-type", "application/json").end(toJson(res)))
      })
    })
  }
  
  private def toJson(any: Any): String = gson.toJson(any)

  private def fromJson[T](json: String, clazz: Class[T]): T = gson.fromJson(json, clazz)

  private def responseJson(req: HttpServerRequest, any: Any) = req.response().end(toJson(any))

  private def parseRequestParams(req: HttpServerRequest, reqClass: Class[_]): Any = {
    def getFieldValue(f: Field): String = {
      req.params.get(f.getName).get.headOption.get
    }

    val reqFieldNames = reqClass.getDeclaredFields().map(f => getFieldValue(f))
    val cs = reqClass.getConstructors()
    val requestObject = cs(0).newInstance(reqFieldNames.toArray: _*)
    requestObject
  }
}
