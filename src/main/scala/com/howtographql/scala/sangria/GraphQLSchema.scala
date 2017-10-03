package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Link
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._
import sangria.macros.derive._

object GraphQLSchema {

  //#
  implicit val LinkType = deriveObjectType[Unit, Link]()

  //#

  val Id = Argument("id", IntType)
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => c.ctx.dao.getLink(c.arg(Id))
      ),
      Field("links",
        ListType(LinkType),
        arguments = List(Argument("ids", ListInputType(IntType))),
        resolve = c => c.ctx.dao.getLinks(c.arg[Seq[Int]]("ids"))
      )
    )
  )

  //#
  val SchemaDefinition = Schema(QueryType)
}
