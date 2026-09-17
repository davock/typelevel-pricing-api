# scala-typelevel-cdk-skeleton

Multi-module sbt skeleton wiring together:

- **Typelevel runtime**: cats-effect 3, fs2, http4s (ember)
- **circe** for JSON serde (via http4s-circe + smithy4s-generated codecs)
- **Smithy / smithy4s** for the API contract (`modules/api/src/main/smithy/api.smithy`),
  codegen'd into Scala types + circe codecs + http4s bindings
- **ciris** for typed, env-based config loading (`modules/server/.../config/AppConfig.scala`)
- **natchez** for distributed tracing, wrapping the http4s routes
  (`modules/server/.../tracing/Tracing.scala`)
- **chimney** for domain <-> DTO transforms (`modules/domain/.../Transforms.scala`)
- **sbt-native-packager** for Docker image packaging (`server/Docker/stage` / `publishLocal`)
- **AWS CDK** (Java bindings, `modules/cdk`) provisioning a Fargate service running that image
- **LocalStack** for running the whole stack locally without touching real AWS

## Layout

```
build.sbt                          root aggregate build
project/plugins.sbt                sbt-native-packager, sbt-assembly, smithy4s, scalafmt, scalafix, bloop
modules/domain/                    pure domain models + chimney transforms
modules/api/                       Smithy model -> smithy4s codegen (circe + http4s)
modules/server/                    cats-effect/fs2/http4s app: Main, config, tracing, routes
modules/cdk/                       AWS CDK app (Java) -> ECS/Fargate stack for the server image
docker-compose.yml                 LocalStack container
scripts/aws-cli-setup.sh           configure aws-cli + cdk against real AWS
scripts/localstack-setup.sh        start LocalStack, configure awslocal/cdklocal
```

## Prerequisites

- JDK 21, sbt 1.10+ (`project/build.properties` pins the sbt version)
- Docker (for `sbt-native-packager` image builds and for LocalStack)
- Node.js (for the `aws-cdk` / `aws-cdk-local` CLIs, which drive the Java CDK app in `modules/cdk`)
- Python 3 + pip (only needed for `awscli-local`/`awslocal`)

