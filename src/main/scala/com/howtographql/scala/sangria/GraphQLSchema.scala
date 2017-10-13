package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import com.howtographql.scala.sangria.models.{Link, User, Vote}
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._
import sangria.macros.derive._
import sangria.validation.Violation

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


  implicit val LinkType = deriveObjectType[Unit, Link]()

  implicit val linkHasId = HasId[Link, Int](_.id)
  val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )

  implicit val UserType = deriveObjectType[Unit, User]()
  implicit val userHasId = HasId[User, Int](_.id)

  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  implicit val VoteType = deriveObjectType[Unit, Vote]()
  implicit val voteHasId = HasId[Vote, Int](_.id)

  val votesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids)
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
