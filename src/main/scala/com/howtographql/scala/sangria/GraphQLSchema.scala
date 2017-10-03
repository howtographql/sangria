package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Link
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._
import sangria.macros.derive._

object GraphQLSchema {

  implicit val LinkType = deriveObjectType[Unit, Link]()

  implicit val linkHasId = HasId[Link, Int](_.id)
  val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )




  val Id = Argument("id", IntType)
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
        arguments = List(Argument("ids", ListInputType(IntType))),
        resolve = c => linksFetcher.deferSeq(c.arg[Seq[Int]]("ids"))
      )
    )
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher)
  //#
  val SchemaDefinition = Schema(QueryType)
}
