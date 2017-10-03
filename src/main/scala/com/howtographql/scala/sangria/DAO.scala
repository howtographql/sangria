package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.DBSchema._
import com.howtographql.scala.sangria.models.Link
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class DAO(db: Database) {

  def allLinks = db.run(Links.result)

  def getLinks(ids: Seq[Int]): Future[Seq[Link]] = {
    println(s"get links: ${ids.mkString(",")}")
    db.run(
      Links.filter(_.id inSet ids).result
    )
  }
}
