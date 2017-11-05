package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.DBSchema._
import com.howtographql.scala.sangria.models.{Link, User, Vote}
import sangria.execution.deferred.{RelationIds, SimpleRelation}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future
import scala.language.postfixOps

class DAO(db: Database) {

  def allLinks = db.run(Links.result)

  def getLinks(ids: Seq[Int]): Future[Seq[Link]] = {
    println(s"get links: ${ids.mkString(",")}")
    db.run(
      Links.filter(_.id inSet ids).result
    )
  }

  def getLinksByUserIds(ids: Seq[Int]): Future[Seq[Link]] = {
    db.run {
      Links.filter(_.postedBy inSet ids).result
    }
  }

  def getUsers(ids: Seq[Int]): Future[Seq[User]] = {
    db.run(
      Users.filter(_.id inSet ids).result
    )
  }

  def getVotes(ids: Seq[Int]): Future[Seq[Vote]] = {
    db.run(
      Votes.filter(_.id inSet ids).result
    )
  }

  def getVotesByRelationIds(rel: RelationIds[Vote]): Future[Seq[Vote]] =
    db.run(
      Votes.filter { vote =>
        rel.rawIds.collect({
          case (SimpleRelation("byUser"), ids: Seq[Int]) => vote.userId inSet ids
          case (SimpleRelation("byLink"), ids: Seq[Int]) => vote.linkId inSet ids
        }).foldLeft(true: Rep[Boolean])(_ || _)

      } result
    )

}
