package com.example.server.tracing

import cats.effect.{Async, Resource, Sync}
import cats.effect.std.{Env, Random}
import com.example.server.config.TracingConfig
import fs2.io.net.Network
import natchez.{EntryPoint, Trace}
import natchez.xray.XRay
import org.http4s.HttpRoutes
import natchez.http4s.NatchezMiddleware

/** Wires up a natchez `EntryPoint`. Swap `XRay` for `natchez.honeycomb.Honeycomb`,
  * `natchez.jaeger.Jaeger`, `natchez.opentelemetry.OpenTelemetry`, etc. depending on
  * where traces should land — the rest of the app only depends on `Trace[F]`,
  * so the backend is a one-file change.
  */
object Tracing:

  def entryPoint[F[_]: Async: Network](cfg: TracingConfig): Resource[F, EntryPoint[F]] =
    given Env[F] = Env.make[F]
    given Random[F] = Random.javaUtilConcurrentThreadLocalRandom[F]
    XRay.entryPoint[F]()

  /** Wrap an http4s `HttpRoutes` so every request gets a root span, and
    * downstream code can add child spans via `Trace[F].span("name")(...)`.
    */
  def middleware[F[_]: Sync: Trace](routes: HttpRoutes[F]): HttpRoutes[F] =
    NatchezMiddleware.server(routes)
