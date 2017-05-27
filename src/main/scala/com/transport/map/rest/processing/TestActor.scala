package com.transport.map.rest.processing

import akka.actor.Actor
import kantan.csv.ops._
import kantan.csv.generic._

import scala.io.{Source, StdIn}
import com.transport.map.rest.schedule.Stops
import kantan.codecs.Result.{Failure, Success}

import scala.collection.mutable.ListBuffer

/**
  * Created by artsiom.chuiko on 23/02/2017.
  */
class TestActor extends Actor {
  override def receive: Receive = {
    case _ => ???
  }
}

object TestApp {
  val content = Source.fromFile("src/main/resources/stops.csv", "UTF-8").mkString
  val parsed = content.asCsvReader[Stops](';', true).filter {
    case Success(_) => true
    case Failure(_) => false
  }.map {
    case Success(stop) => stop
  }
  val stops = parsed.toList
  stops.foreach { stop =>
    stop.stops match {
      case Some(list) => {
        list.split(",").map(_.trim).foreach { id =>
          val sibling = stops.find(s => s.id == id.toInt)
          sibling match {
            case Some(sib) => stop.linked += sib
            case None => println(s"sibling is none for $id")
          }
        }
      }
      case None => println(s"stops is none for ${stop.id}")
    }
  }

  println(s"Size of stops is ${stops.length}")

  val ids = ListBuffer.empty[Int]
  val route = ListBuffer.empty[Stops]

  def printStops(stop: Stops): Unit = {
    println(stop)
    route += stop
    ids += stop.id
    stop.linked.filter(s => !ids.contains(s.id)).foreach(printStops _)
  }

//  stops.foreach { stop => println(stop.linked)}
  val stop = stops.find(s => s.id == 14808)
  stop match {
    case Some(s) => printStops(s)
    case None => ;
  }

  println(s"Size is ${route.length}")
  val map = route.groupBy(s => s.id)
  println(s"size of map is ${map.size}")
  map.foreach { entry =>
    if (entry._2.length > 2) {
      println(entry._2)
    }
  }
}
