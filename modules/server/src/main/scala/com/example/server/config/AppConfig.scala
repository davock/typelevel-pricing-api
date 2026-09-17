package com.example.server.config

import cats.effect.Async
import cats.syntax.all.*
import ciris.*

final case class HttpConfig(host: String, port: Int)

final case class TracingConfig(
    serviceName: String,
    // e.g. "http://localhost:4318/v1/traces" when pointed at an OTel collector,
    // or left as-is for natchez-xray / other backends.
    endpoint: Option[String]
)

final case class AwsConfig(
    region: String,
    // When set (e.g. http://localhost:4566), AWS SDK clients target LocalStack
    // instead of real AWS. Leave unset in production.
    endpointOverride: Option[String]
)

final case class AppConfig(
    http: HttpConfig,
    tracing: TracingConfig,
    aws: AwsConfig
)

object AppConfig:

  private val httpConfig: ConfigValue[Effect, HttpConfig] =
    (
      env("HTTP_HOST").default("0.0.0.0"),
      env("HTTP_PORT").as[Int].default(8080)
    ).parMapN(HttpConfig.apply)

  private val tracingConfig: ConfigValue[Effect, TracingConfig] =
    (
      env("SERVICE_NAME").default("example-server"),
      env("TRACING_ENDPOINT").option
    ).parMapN(TracingConfig.apply)

  private val awsConfig: ConfigValue[Effect, AwsConfig] =
    (
      env("AWS_REGION").default("us-east-1"),
      // e.g. AWS_ENDPOINT_URL=http://localhost:4566 when running against LocalStack.
      env("AWS_ENDPOINT_URL").option
    ).parMapN(AwsConfig.apply)

  def load[F[_]: Async]: F[AppConfig] =
    (httpConfig, tracingConfig, awsConfig)
      .parMapN(AppConfig.apply)
      .load[F]
