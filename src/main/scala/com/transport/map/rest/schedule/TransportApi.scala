package com.transport.map.rest.schedule

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Created by arttsiom.chuiko on 19/02/2017.
  */
trait TransportApi {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  lazy val transApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("services.schedule.host"), config.getInt("services.schedule.port"))

  def transApiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(transApiConnectionFlow).runWith(Sink.head)
}
