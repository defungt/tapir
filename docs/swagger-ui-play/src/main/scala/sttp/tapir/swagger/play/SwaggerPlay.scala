package sttp.tapir.swagger.play

import java.util.Properties

import com.typesafe.config.ConfigFactory
import play.api.http.{DefaultFileMimeTypes, FileMimeTypes, FileMimeTypesConfiguration, MimeTypes}
import play.api.mvc.{ActionBuilder, AnyContent, Request}
import play.api.mvc.Results._
import play.api.routing.Router.Routes
import play.api.routing.sird._

import scala.concurrent.ExecutionContext

/**
  * Usage: add `new SwaggerAkka(yaml).routes` to your akka-http routes. Docs will be available using the `/docs` path.
  *
  * @param actionBuilder  The ActionBuilder instance to use. Depending on your implementation, this can come from:
  *                       sttp.tapir.server.play.PlayServerOptions.defaultActionBuilder
  *                       play.api.mvc.ControllerComponents.actionBuilder
  *                       play.api.BuiltInComponents.defaultActionBuilder
  * @param yaml           The yaml with the OpenAPI documentation.
  * @param contextPath    The context in which the documentation will be served. Defaults to `docs`, so the address
  *                       of the docs will be `/docs`.
  * @param yamlName       The name of the file, through which the yaml documentation will be served. Defaults to `docs.yaml`.
**/
class SwaggerPlay(
  actionBuilder: ActionBuilder[Request, AnyContent],
  yaml: String,
  contextPath: String = "docs",
  yamlName: String = "docs.yaml"
)(implicit ec: ExecutionContext) {
  private implicit val swaggerUIFileMimeTypes: FileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration(Map(
    "html" -> MimeTypes.HTML,
    "css" -> MimeTypes.CSS,
    "js" -> MimeTypes.JAVASCRIPT,
    "png" -> "image/png"
  )))
  private val swaggerVersion: String = {
    ConfigFactory.load()
    val p = new Properties()
    val pomProperties = getClass.getResourceAsStream("/META-INF/maven/org.webjars/swagger-ui/pom.properties")
    try p.load(pomProperties)
    finally pomProperties.close()
    p.getProperty("version")
  }
  private val redirectPath = s"/$contextPath/index.html?url=/$contextPath/$yamlName"
  private val resourcePathPrefix = s"META-INF/resources/webjars/swagger-ui/$swaggerVersion"

  def routes: Routes = {
    case GET(p"/$path") if path == contextPath => actionBuilder {
      MovedPermanently(redirectPath)
    }
    case GET(p"/$path/$file") if path == contextPath && file == yamlName => actionBuilder {
      Ok(yaml).as("text/yaml")
    }
    case GET(p"/$path/$file") if path == contextPath => actionBuilder {
      Ok.sendResource(s"$resourcePathPrefix/$file")
    }
  }
}
