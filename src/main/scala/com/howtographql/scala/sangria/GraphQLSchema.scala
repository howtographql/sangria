package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Link
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._
import sangria.macros.derive._

object GraphQLSchema {

  //#
  implicit val LinkType = deriveObjectType[Unit, Link]()

  //#
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks)
    )
  )

  //#
  val SchemaDefinition = Schema(QueryType)
}
