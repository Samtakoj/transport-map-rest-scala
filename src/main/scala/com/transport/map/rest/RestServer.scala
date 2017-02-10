package com.transport.map.rest

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.transport.map.rest.status.{StatusEndpointRouterFactory}
import com.typesafe.config.{Config, ConfigFactory}
import io.github.lhotari.akka.http.health.HealthEndpoint

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
  * Created by artsiom.chuiko on 08/02/2017.
  */
trait Server {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter
}



object RestServer extends App with Server {
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
    StatusEndpointRouterFactory.create("Transport-Map-Rest", "0.0.1", s"http://$interface:$port/")

  val binding = Http().bindAndHandle(route, interface, port)
  println(s"Rest server online at http://$interface:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
}
