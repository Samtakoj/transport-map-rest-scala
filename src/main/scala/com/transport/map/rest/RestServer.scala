package com.transport.map.rest

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.transport.map.rest.schedule.TransportApi
import com.transport.map.rest.status.StatusType.Green
import com.transport.map.rest.status.{Status, StatusChecker, StatusEndpointRouterFactory}
import com.typesafe.config.{Config, ConfigFactory}
import io.github.lhotari.akka.http.health.HealthEndpoint

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
  * Created by artsiom.chuiko on 08/02/2017.
  */
trait AkkaAware {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter
}
trait Server extends AkkaAware {

}

object RestServer extends App with TransportApi {
  override implicit val system = ActorSystem()
  override implicit def executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override def config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  val route = path("schedule") {
    get {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Test implementation</h1>"))
    }
  } ~
    HealthEndpoint.createDefaultHealthRoute() ~
    StatusEndpointRouterFactory.create("Transport-Map-Rest", "0.0.1", s"http://$interface:$port/", List(new StatusChecker {
      override def check() = Status("TestDatabaseComponent1", "0.0.1", "jdbc:sqlserver://CHESQL040;database=Test", Green, List.empty)
    })) ~
    createStopsRoute() ~
    createRoutesRoute() ~
    createTimesRoute()

  val binding = Http().bindAndHandle(route, interface, port)
  println(s"Rest server online at http://$interface:$port/\nPress RETURN to stop...")
//  StdIn.readLine()
//  binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
}
