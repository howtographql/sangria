package com.howtographql.scala.sangria.models

import akka.http.scaladsl.model.DateTime
import sangria.execution.deferred.HasId

trait Identifiable {
  val id: Int
}

object Identifiable {
  implicit def hasId[T <: Identifiable]: HasId[T, Int] = HasId(_.id)
}

case class Link(id: Int, url: String, description: String, createdAt: DateTime = DateTime.now) extends Identifiable

case class User(id: Int, name: String, email: String, password: String, createdAt: DateTime = DateTime.now) extends Identifiable

case class Vote(id: Int, userId: Int, linkId: Int, createdAt: DateTime = DateTime.now) extends Identifiable
