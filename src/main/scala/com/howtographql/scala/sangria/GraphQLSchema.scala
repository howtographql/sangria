package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import com.howtographql.scala.sangria.models.{Identifiable, Link, User, Vote}
import sangria.ast.StringValue
import sangria.execution.deferred._
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._
import sangria.macros._
import sangria.macros.derive._
import sangria.validation.Violation
import sangria.schema.{Argument, BigDecimalType, BooleanType, Context, DeferredValue, Field, FloatType, ListInputType, ListType, LongType, ObjectType, OptionInputType, OptionType, StringType, fields, interfaces}


object GraphQLSchema {

  case object DateTimeCoerceViolation extends Violation {
    override def errorMessage: String = "Error parsing DateTime"
  }

  implicit val GraphQLDateTime = ScalarType[DateTime]("DateTime",
    coerceOutput = (dt, _) => dt.toString,
    coerceInput = {
      case StringValue(dt, _,_ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = {
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  val IdentifiableType: InterfaceType[MyContext, Identifiable] = InterfaceType(
    "Identifiable",
    fields[MyContext, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  val linkByUserRel = Relation[Link, Int]("byUser", l => Seq(l.postedBy))
  val voteByUserRel = Relation[Vote, Int]("byUser", v => Seq(v.userId))

  implicit lazy val UserType: ObjectType[MyContext, User] = deriveObjectType[MyContext, User](
    Interfaces[MyContext, User](IdentifiableType),
    AddFields(
      Field("links", ListType(LinkType), resolve = c =>  linksFetcher.deferRelSeq(linkByUserRel, c.value.id)),
      Field("votes", ListType(VoteType), resolve = c =>  votesFetcher.deferRelSeq(voteByUserRel, c.value.id))
    )
  )

  implicit lazy val LinkType = deriveObjectType[MyContext, Link](
    Interfaces[MyContext, Link](IdentifiableType),
    ReplaceField("postedBy",
      Field("postedBy", UserType, resolve = c => usersFetcher.defer(c.value.postedBy))
    )
  )

  val linksFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids),
    (ctx: MyContext, ids: RelationIds[Link]) => ctx.dao.getLinksByUserIds(ids(linkByUserRel))
  )

  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  implicit val VoteType = deriveObjectType[MyContext, Vote](
    Interfaces[MyContext, Vote](IdentifiableType),
    ExcludeFields("userId"),
    AddFields(Field("user",  UserType, resolve = c => usersFetcher.defer(c.value.userId)))
  )

  val votesFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids),
    (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByUserIds(ids(voteByUserRel))
  )

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.deferOpt(c.arg[Int]("id"))
      ),
      Field("links",
        ListType(LinkType),
        arguments = List(Ids),
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      ),
      Field("users",
        ListType(UserType),
        arguments = List(Ids),
        resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),
      Field("votes",
        ListType(VoteType),
        arguments = List(Ids),
        resolve = c => votesFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)
  //#
  val SchemaDefinition = Schema(QueryType)
}
