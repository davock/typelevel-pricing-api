package com.example.server

import cats.effect.{IO, IOApp, ExitCode, Resource}
import com.comcast.ip4s.*
import com.example.server.config.AppConfig
import com.example.server.routes.HealthRoutes
import com.example.server.tracing.Tracing
import natchez.Trace
import org.http4s.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] =
    resources.use(_ => IO.never).as(ExitCode.Success)

  private def resources: Resource[IO, Server] =
    for
      cfg        <- Resource.eval(AppConfig.load[IO])
      entryPoint <- Tracing.entryPoint[IO](cfg.tracing)
      server     <- entryPoint.root("http-server").flatMap { rootSpan =>
                      Resource.eval(Trace.ioTrace(rootSpan)).flatMap { tr =>
                        given Trace[IO] = tr
                        val routes = Tracing.middleware(HealthRoutes[IO].routes)
                        EmberServerBuilder
                          .default[IO]
                          .withHost(Host.fromString(cfg.http.host).getOrElse(host"0.0.0.0"))
                          .withPort(Port.fromInt(cfg.http.port).getOrElse(port"8080"))
                          .withHttpApp(routes.orNotFound)
                          .build
                      }
                    }
    yield server
