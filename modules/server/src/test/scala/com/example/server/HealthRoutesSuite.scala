package com.example.server

import cats.effect.IO
import com.example.server.routes.HealthRoutes
import munit.CatsEffectSuite
import org.http4s.{Method, Request, Status}
import org.http4s.implicits.*

class HealthRoutesSuite extends CatsEffectSuite:

  test("GET /health returns 200") {
    val app = HealthRoutes[IO].routes.orNotFound
    app
      .run(Request[IO](Method.GET, uri"/health"))
      .map(_.status)
      .assertEquals(Status.Ok)
  }
