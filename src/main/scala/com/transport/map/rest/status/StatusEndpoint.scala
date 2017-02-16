package com.transport.map.rest.status

import java.util.concurrent.atomic.AtomicBoolean

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.transport.map.rest.status.StatusType._
import spray.json.{CollectionFormats, DefaultJsonProtocol, _}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by artsiom.chuiko on 10/02/2017.
  */
case class Status(name: String, version: String, location: String, status: StatusType, dependencies: List[Status] = List.empty)

sealed trait StatusType {
  def name: String
}

object StatusType {
  case object Green extends StatusType { override def name = "Green" }
  case object Yellow extends StatusType { override def name = "Yellow" }
  case object Red extends StatusType { override def name = "Red" }
}

object StatusJsonProtocol extends DefaultJsonProtocol with CollectionFormats {
  implicit object StatusJsonFormat extends RootJsonFormat[Status] {
    override def write(obj: Status): JsValue = JsObject(
      "name" -> JsString(obj.name),
      "version" -> JsString(obj.version),
      "location" -> JsString(obj.location),
      "status" -> JsString(obj.status.name),
      "dependencies" -> JsArray(obj.dependencies.map(_.toJson).toVector)
    )

    override def read(json: JsValue): Status = ???
  }
}

sealed trait StatusInfoAware {
  protected def name: String
  protected def version: String
  protected def location: String
}

trait StatusChecker {
  def start(): Unit = {}
  def stop(): Unit = {}
  def check(): Status
}

trait StatusEndpoint extends StatusInfoAware {
  private val started = new AtomicBoolean(false)

  protected lazy val components = createComponents

  protected def createComponents(): List[StatusChecker]

  def createStatusRoute()(implicit executor: ExecutionContext): Route = get {
    path("status") {
      completeStatusCheck
    }
  }

  import StatusJsonProtocol._
  def createHealthResponse: HttpResponse = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, check().toJson.prettyPrint))

  private lazy val statusResponse: HttpResponse = createHealthResponse

  def completeStatusCheck(implicit executor: ExecutionContext) = complete {
    Future {
      statusResponse
    }
  }

  def start(): Unit = if (started.compareAndSet(false, true)) components.foreach(_.start())

  def stop(): Unit = if (started.compareAndSet(true, false)) components.foreach(_.stop())

  def check() = {
    start()
    val dependencies = components.map(_.check())
    val status = dependencies.exists(_.status == Red) match {
      case true => Red
      case false => Green
    }
    Status(name, version, location, status, dependencies)
  }
}

object StatusEndpointRouterFactory {
  def create(nameStr: String, versionStr: String, locationStr: String, checkers: List[StatusChecker] = List.empty)(implicit executor: ExecutionContext) = new StatusEndpoint {
    override protected def createComponents() = checkers
    override protected def location = locationStr
    override protected def name = nameStr
    override protected def version = versionStr
  }.createStatusRoute()
}