package com.transport.map.rest.schedule

import java.io.IOException

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.transport.map.rest.AkkaAware
import kantan.csv.ops._
import kantan.csv.generic._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by artsiom.chuiko on 19/02/2017.
  */
trait TransportApi extends AkkaAware{

  lazy val transApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("services.schedule.host"), config.getInt("services.schedule.port"))

  def transApiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(transApiConnectionFlow).runWith(Sink.head)

  def fetchData(path: String): Future[Either[String, String]] = {
    transApiRequest(RequestBuilding.Get(path)).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[String].map(Right(_))
        case BadRequest => Future.successful(Left(s"${response.status} status was received"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Transport Api request failed with status code ${response.status} and entity $entity"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }

  def fetchStops() = complete {
    fetchData(config.getString("services.schedule.stops")).map[ToResponseMarshallable] {
      case Right(data) => { data.asCsvReader[Stops](';', true).foreach(println _); data }
      case Left(errorMessage) => BadRequest -> errorMessage
    }
  }

  def fetchRoutes() = complete {
    fetchData(config.getString("services.schedule.routes")).map[ToResponseMarshallable] {
      case Right(data) => data
      case Left(errorMessage) => BadRequest -> errorMessage
    }
  }

  def fetchTimes() = complete {
    fetchData(config.getString("services.schedule.times")).map[ToResponseMarshallable] {
      case Right(data) => data
      case Left(errorMessage) => BadRequest -> errorMessage
    }
  }

  def createStopsRoute()(implicit executor: ExecutionContext): Route = get {
    path("stops") {
      fetchStops()
    }
  }

  def createRoutesRoute()(implicit executor: ExecutionContext): Route = get {
    path("routes") {
      fetchRoutes()
    }
  }

  def createTimesRoute()(implicit executor: ExecutionContext): Route = get {
    path("times") {
      fetchTimes()
    }
  }
}

case class Stops(id: Int, city: Int, area: Int, street: Option[String],
                 name: Option[String], info: Option[String], lng: Option[Long], lat: Option[Long],
                 stops: Option[String]) {
  val linked = scala.collection.mutable.ListBuffer.empty[Stops]
}
