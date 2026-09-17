package com.example.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateServiceProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

/**
 * Minimal stack: builds the Docker image produced by
 * `sbt server/Docker/publishLocal` (see build.sbt) and runs it as a
 * Fargate service behind an ALB.
 *
 * This is intentionally small — extend with the DynamoDB table / SQS queue /
 * whatever backs the WidgetService operations defined in the Smithy model,
 * and wire the ALB's env vars (AWS_REGION, AWS_ENDPOINT_URL for LocalStack,
 * TRACING_ENDPOINT, ...) to match modules/server/.../AppConfig.scala.
 */
public class ServiceStack extends Stack {

    public ServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "ServiceVpc")
                .maxAzs(2)
                .natGateways(1)
                .build();

        Cluster cluster = Cluster.Builder.create(this, "ServiceCluster")
                .vpc(vpc)
                .build();

        DockerImageAsset image = DockerImageAsset.Builder.create(this, "ServerImage")
                // Points at the Dockerfile sbt-native-packager generates under
                // modules/server/target/docker/stage after `sbt server/Docker/stage`.
                .directory("../server/target/docker/stage")
                .build();

        ApplicationLoadBalancedFargateService.Builder.create(this, "ServiceFargate")
                .cluster(cluster)
                .cpu(256)
                .memoryLimitMiB(512)
                .desiredCount(1)
                .publicLoadBalancer(true)
                .taskImageOptions(
                        software.amazon.awscdk.services.ecs.patterns
                                .ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromDockerImageAsset(image))
                                .containerPort(8080)
                                .build())
                .build();
    }
}
