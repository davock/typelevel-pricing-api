package com.example.cdk;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;


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


        Table pricingTable = Table.Builder.create(this, "PricingTable")
                .tableName("pricing")
                .partitionKey(Attribute.builder().name("sku").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        DockerImageAsset image = DockerImageAsset.Builder.create(this, "ServerImage")
                // Points at the Dockerfile sbt-native-packager generates under
                // modules/server/target/docker/stage after `sbt server/Docker/stage`.
                .directory("modules/server/target/docker/stage")
                .build();

        FargateTaskDefinition taskDef = FargateTaskDefinition.Builder.create(this, "PricingTaskDef")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        taskDef.addContainer("ServerContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromDockerImageAsset(image))
                .portMappings(List.of(PortMapping.builder().containerPort(8080).build()))
                .environment(Map.of(
                        "PRICING_TABLE_NAME", pricingTable.getTableName(),
                        "AWS_REGION", this.getRegion()
                ))
                .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder().streamPrefix("pricing-server").build()))
                .build()
        );

        FargateService service = FargateService.Builder.create(this, "PricingFargateService")
                .cluster(cluster)
                .taskDefinition(taskDef)
                .desiredCount(1)
                .build();

        pricingTable.grantReadData(taskDef.getTaskRole());

        ApplicationLoadBalancer alb = ApplicationLoadBalancer.Builder.create(this, "PricingAlb")
                .vpc(vpc)
                .internetFacing(true)
                .build();

        ApplicationTargetGroup targetGroup = ApplicationTargetGroup.Builder.create(this, "PricingTargetGroup")
                .vpc(vpc)
                .port(8080)
                .protocol(ApplicationProtocol.HTTP)
                .targets(List.of(service))
                .healthCheck(HealthCheck.builder()
                        .path("/health")
                        .interval(Duration.seconds(30))
                        .build()
                )
                .build();

        alb.addListener("PricingListener", BaseApplicationListenerProps.builder()
                .port(80)
                .defaultTargetGroups(List.of(targetGroup))
                .build()
        );

        service.getConnections().allowFrom(alb, Port.tcp(8080));
        
    }
}
