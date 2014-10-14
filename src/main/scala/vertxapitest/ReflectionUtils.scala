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
 * Helper class for instantiate case classes
 */
object ReflectionUtils { 

  def instantiate[T](f: Field => Object)(implicit t: Manifest[T]) = {
    val consArgs = t.erasure.getDeclaredFields().filter(f => f.getName().charAt(0) != '$').map(f)
    val cons = t.erasure.getConstructors()
    //println("Calling constructor", cons(0) ," with: ", consArgs.toList)
    val instance = cons(0).newInstance(consArgs.toArray: _*)
    instance
  }
  
}
