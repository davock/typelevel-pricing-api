package com.example.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * Entry point invoked by the CDK CLI (see modules/cdk/cdk.json).
 *
 * Run against real AWS:
 *   cdk deploy --app "sbt -error 'cdk/runMain com.example.cdk.CdkApp'"
 *
 * Run against LocalStack (with cdklocal / awslocal installed, see scripts/localstack-setup.sh):
 *   cdklocal deploy --app "sbt -error 'cdk/runMain com.example.cdk.CdkApp'"
 */
public final class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        String region = System.getenv().getOrDefault("CDK_DEFAULT_REGION", "us-east-1");

        Environment env = Environment.builder()
                .account(account)
                .region(region)
                .build();

        new ServiceStack(app, "ExampleServiceStack", StackProps.builder()
                .env(env)
                .description("Skeleton stack for the Typelevel/http4s server (modules/server)")
                .build());

        app.synth();
    }
}
