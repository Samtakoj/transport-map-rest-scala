package com.transport.map.rest.status

import java.util.concurrent.atomic.AtomicBoolean

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.transport.map.rest.status.StatusType._
import spray.json._
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by artsiom.chuiko on 10/02/2017.
  */
case class Status(name: String, version: String, location: String, status: String, dependencies: List[Status])

sealed trait StatusType

object StatusType {
  case object Green extends StatusType
  case object Yellow extends StatusType
  case object Red extends StatusType
}

object StatusJsonProtocol extends DefaultJsonProtocol with CollectionFormats{
  implicit val statusFormat = lazyFormat(jsonFormat5(Status))
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
    Status(name, version, location, status.getClass.getName, dependencies)
  }
}

object StatusEndpointRouterFactory {
  def create(name: String, version: String, location: String)(implicit executor: ExecutionContext) = new StatusEndpoint {
    override protected def createComponents() = List.empty[StatusChecker]
    override protected def location = location
    override protected def name = name
    override protected def version = version
  }.createStatusRoute()
}