import com.typesafe.sbt.packager.docker.DockerChmodType

ThisBuild / scalaVersion := "3.8.4"
ThisBuild / organization := "com.example"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all"
)

// ---- Versions -------------------------------------------------------------
val CatsEffectV = "3.5.7"
val Fs2V        = "3.11.0"
val Http4sV     = "0.23.30"
val JsoniterV   = "2.27.6" // pinned to match the version smithy4s-json pulls in transitively
val NatchezV      = "0.3.7"
val NatchezHttp4sV = "0.6.1" // natchez-http4s is released on its own version line; 0.6.1 pairs with natchez-core 0.3.7 and http4s 0.23.30
val CirisV      = "3.7.0"
val ChimneyV    = "1.6.0"
val Smithy4sV   = "0.18.24"
val MunitV      = "1.0.3"
val MunitCEV    = "2.0.0"
val LogbackV    = "1.5.12"
val Log4CatsV   = "2.7.0"
val WeaverV     = "0.8.4"

// ---- Common settings --------------------------------------------------
lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalameta"      %% "munit"            % MunitV     % Test,
    "org.typelevel"       %% "munit-cats-effect" % MunitCEV   % Test,
    // weaver-core pulls in a hardcoded org.scala-lang:scala-reflect:2.13.x dependency
    // (a leftover from its cross-platform macro support) that sbt's default Scala-version
    // override then tries to force to this build's scalaVersion — which fails to resolve
    // since scala-reflect was never published for Scala 3. It isn't used on the JVM 3 path,
    // so it's safe to exclude.
    ("com.disneystreaming" %% "weaver-cats"      % WeaverV    % Test)
      .exclude("org.scala-lang", "scala-reflect")
  ),
  testFrameworks ++= Seq(
    new TestFramework("munit.Framework"),
    new TestFramework("weaver.framework.CatsEffect")
  )
)

// ---- domain: pure models + chimney DTO transforms ------------------------
lazy val domain = (project in file("modules/domain"))
  .settings(commonSettings)
  .settings(
    name := "domain",
    libraryDependencies ++= Seq(
      "io.scalaland" %% "chimney" % ChimneyV
    )
  )

// ---- api: Smithy model + generated jsoniter/http4s bindings ---------------
lazy val api = (project in file("modules/api"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core"  % Smithy4sV,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % Smithy4sV
    )
  )

// ---- server: cats-effect / fs2 / http4s runtime, ciris config, natchez ----
lazy val server = (project in file("modules/server"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(domain, api)
  .settings(commonSettings)
  .settings(
    name := "server",
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "cats-effect"          % CatsEffectV,
      "co.fs2"          %% "fs2-core"             % Fs2V,
      "co.fs2"          %% "fs2-io"               % Fs2V,
      "org.http4s"      %% "http4s-ember-server"  % Http4sV,
      "org.http4s"      %% "http4s-ember-client"  % Http4sV,
      "org.http4s"      %% "http4s-dsl"           % Http4sV,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % JsoniterV,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % JsoniterV % Provided,
      "is.cir"          %% "ciris"                % CirisV,
      "org.tpolecat"    %% "natchez-core"         % NatchezV,
      "org.tpolecat"    %% "natchez-http4s"        % NatchezHttp4sV,
      "org.tpolecat"    %% "natchez-xray"          % NatchezV, // swap/add natchez-honeycomb, natchez-jaeger, etc. as needed
      "org.typelevel"   %% "log4cats-slf4j"       % Log4CatsV,
      "ch.qos.logback"   % "logback-classic"      % LogbackV
    ),
    // ---- sbt-native-packager: build a runnable Docker image -----
    Docker / packageName := "example-server",
    dockerBaseImage      := "eclipse-temurin:21-jre-jammy",
    dockerExposedPorts   := Seq(8080),
    dockerChmodType      := DockerChmodType.UserGroupWriteExecute,
    Docker / version     := (ThisBuild / version).value
  )

// ---- cdk: AWS CDK app (Java bindings) provisioning the stack ---------------
lazy val cdk = (project in file("modules/cdk"))
  .settings(
    name := "cdk",
    autoScalaLibrary := false,
    crossPaths := false,
    libraryDependencies ++= Seq(
      "software.amazon.awscdk" % "aws-cdk-lib" % "2.170.0",
      "software.constructs"    % "constructs"  % "10.4.2"
    )
  )

lazy val root = (project in file("."))
  .aggregate(domain, api, server, cdk)
  .settings(
    name := "scala-typelevel-cdk-skeleton",
    publish / skip := true
  )
