package com.example.domain

import io.scalaland.chimney.dsl.*
import java.time.Instant
import java.util.UUID
import scala.util.Try

/** Chimney-powered transforms between the domain model and its DTO.
  *
  * `.transform` handles the identical fields automatically; `.withFieldComputed`
  * fills in the fields whose representation differs (UUID <-> String,
  * Instant <-> ISO-8601 String).
  */
object Transforms:

  extension (w: Widget)
    def toDto: WidgetDto =
      w.into[WidgetDto]
        .withFieldComputed(_.id, _.id.toString)
        .withFieldComputed(_.createdAt, _.createdAt.toString)
        .transform

  extension (dto: WidgetDto)
    def toDomain: Either[Throwable, Widget] =
      Try {
        dto
          .into[Widget]
          .withFieldComputed(_.id, d => UUID.fromString(d.id))
          .withFieldComputed(_.createdAt, d => Instant.parse(d.createdAt))
          .transform
      }.toEither
