package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Link
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps


object DBSchema {

  class LinksTable(tag: Tag) extends Table[Link](tag, "LINKS"){

    def id = column[Int]("ID", O.PrimaryKey)
    def url = column[String]("URL")
    def description = column[String]("DESCRIPTION")

    def * = (id, url, description) <> ((Link.apply _).tupled, Link.unapply)

  }

  val Links = TableQuery[LinksTable]

  /**
    * Load schema and populate sample data withing this Sequence od DBActions
    */
  val databaseSetup = DBIO.seq(
    Links.schema.create,

    Links ++= Seq(
      Link(0, "http://howtographql.com", "Awesome community driven GraphQL tutorial"),
      Link(0, "http://graphql.org", "Official GraphQL webpage"),
      Link(0, "https://facebook.github.io/graphql/", "GraphQL specification")
    )
  )


  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}
