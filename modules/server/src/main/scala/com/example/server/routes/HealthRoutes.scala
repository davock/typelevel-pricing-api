package com.example.server.routes

import cats.effect.Sync
import cats.syntax.all.*
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final case class HealthResponse(status: String)

final class HealthRoutes[F[_]: Sync] extends Http4sDsl[F]:

  val routes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "health" =>
    Sync[F].pure(HealthResponse("ok")).flatMap(Ok(_))
  }

object HealthRoutes:
  def apply[F[_]: Sync]: HealthRoutes[F] = new HealthRoutes[F]
