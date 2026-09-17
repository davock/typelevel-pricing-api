// Packaging: Docker / native-packager for the JVM service
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.10.4")

// Fat-jar assembly (handy for AWS Lambda deployment artifacts from the CDK stack)
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.2.0")

// Smithy -> Scala codegen (models, circe codecs, http4s server/client stubs)
addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.18.24")

// Formatting / linting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.13.0")

// Nice dependency graph / update reports while iterating on the Typelevel stack
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "2.0.6")
