package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.{AuthenticationException, User}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class MyContext(dao: DAO, user: Option[User]){
  def login(email: String, password: String): User = {
    val userOpt = Await.result(dao.authenticate(email, password), Duration.Inf)
    userOpt.getOrElse(
      throw AuthenticationException("email or password are incorrect!")
    )
  }
}
