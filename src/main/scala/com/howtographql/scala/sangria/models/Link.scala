package com.howtographql.scala.sangria.models

import akka.http.scaladsl.model.DateTime

case class Link(id: Int, url: String, description: String, createdAt: DateTime = DateTime.now)

case class User(id: Int, name: String, email: String, password: String, createdAt: DateTime = DateTime.now)

case class Vote(id: Int, userId: Int, linkId: Int, createdAt: DateTime = DateTime.now)
