package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import com.howtographql.scala.sangria.models._
import sangria.ast.StringValue
import sangria.execution.{ExceptionHandler, HandledException}
import sangria.execution.deferred._
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._
import sangria.macros._
import sangria.macros.derive._
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
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
  val voteByLinkRel = Relation[Vote, Int]("byLink", v => Seq(v.linkId))

  implicit lazy val UserType: ObjectType[MyContext, User] = deriveObjectType[MyContext, User](
    Interfaces[MyContext, User](IdentifiableType),
    AddFields(
      Field("links", ListType(LinkType), resolve = c => linksFetcher.deferRelSeq(linkByUserRel, c.value.id)),
      Field("votes", ListType(VoteType), resolve = c => votesFetcher.deferRelSeq(voteByUserRel, c.value.id))
    )
  )

  implicit lazy val LinkType = deriveObjectType[MyContext, Link](
    Interfaces[MyContext, Link](IdentifiableType),
    ReplaceField("postedBy",
      Field("postedBy", UserType, resolve = c => usersFetcher.defer(c.value.postedBy))
    ),
    AddFields(
      Field("votes", ListType(VoteType), resolve = c => votesFetcher.deferRelSeq(voteByLinkRel, c.value.id))
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
    (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByRelationIds(ids)
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

  /**
    * Mutations
    */
  import sangria.marshalling.sprayJson._
  import spray.json.DefaultJsonProtocol._

  implicit val authProviderEmailFormat = jsonFormat2(AuthProviderEmail)
  implicit val authProviderSignupDataFormat = jsonFormat1(AuthProviderSignupData)



  implicit val AuthProviderEmailInputType: InputObjectType[AuthProviderEmail] = deriveInputObjectType[AuthProviderEmail](
    InputObjectTypeName("AUTH_PROVIDER_EMAIL")
  )

  implicit val AuthProviderSignupDataInputType: InputObjectType[AuthProviderSignupData] = deriveInputObjectType[AuthProviderSignupData]()


  val NameArg = Argument("name", StringType)
  val AuthProviderArg = Argument("authProvider", AuthProviderSignupDataInputType)

  val UrlArg = Argument("url", StringType)
  val DescArg = Argument("description", StringType)
  val PostedByArg = Argument("postedById", IntType)

  val LinkIdArg = Argument("linkId", IntType)
  val UserIdArg = Argument("userId", IntType)

  val EmailArg = Argument("email", StringType)
  val PasswordArg = Argument("password", StringType)

  val Mutation = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("createUser",
        UserType,
        arguments = NameArg :: AuthProviderArg :: Nil,
        resolve = c => c.ctx.dao.createUser(c.arg(NameArg), c.arg(AuthProviderArg))
      ),
      Field("createLink",
        LinkType,
        arguments = UrlArg :: DescArg :: PostedByArg :: Nil,
        tags = Authorised :: Nil,
        resolve = c => c.ctx.dao.createLink(c.arg(UrlArg), c.arg(DescArg), c.arg(PostedByArg))),
      Field("createVote",
        VoteType,
        arguments = LinkIdArg :: UserIdArg :: Nil,
        resolve = c => c.ctx.dao.createVote(c.arg(LinkIdArg), c.arg(UserIdArg))),
      Field("login",
        UserType,
        arguments = EmailArg :: PasswordArg :: Nil,
        resolve = ctx => UpdateCtx(
          ctx.ctx.login(ctx.arg(EmailArg), ctx.arg(PasswordArg))){ user =>
            ctx.ctx.copy(currentUser = Some(user))
          }
      )
    )
  )



  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)

  val ErrorHandler = ExceptionHandler {
    case (m, AuthenticationException(message)) ⇒ HandledException(message)
    case (m, AuthorisationException(message)) ⇒ HandledException(message)
  }

  //#
  val SchemaDefinition = Schema(QueryType, Some(Mutation))
}
