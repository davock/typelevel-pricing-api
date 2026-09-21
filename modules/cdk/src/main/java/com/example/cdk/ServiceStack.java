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
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSourceProps;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;


public class ServiceStack extends Stack {

    public ServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        Vpc vpc = Vpc.Builder.create(this, "ServiceVpc")
                .maxAzs(2)
                .natGateways(0)
                .build();


        Cluster cluster = Cluster.Builder.create(this, "ServiceCluster")
                .vpc(vpc)
                .build();


        Table pricingTable = Table.Builder.create(this, "PricingTable")
                .tableName("pricing")
                .partitionKey(Attribute.builder().name("sku").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        Table ordersTable = Table.Builder.create(this, "OrdersTable")
                .tableName("orders")
                .partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .stream(StreamViewType.NEW_IMAGE)
                .build();


        Stream orderPricedStream = Stream.Builder.create(this, "OrderPricedEvents")
                .streamName("order-priced-events")
                .shardCount(1)
                .build();

        Function outboxDispatcher = Function.Builder.create(this, "OutboxDispatcherFn")
                .runtime(Runtime.JAVA_21)
                .handler("com.example.streamprocessor.Handler")
                .code(Code.fromAsset("modules/stream-processor/target/scala-3.3.4/stream-processor.jar"))
                .memorySize(512)
                .environment(Map.of(
                        "KINESIS_STREAM_NAME", orderPricedStream.getStreamName(),
                        "AWS_REGION", this.getRegion()
                ))
                .build();
        
        
        outboxDispatcher.addEventSource(new DynamoEventSource(ordersTable, DynamoEventSourceProps.builder()
                .startingPosition(StartingPosition.LATEST)
                .batchSize(10)
                .build()
        ));
        
        orderPricedStream.grantReadWrite(outboxDispatcher);
        
        

        FargateTaskDefinition taskDef = FargateTaskDefinition.Builder.create(this, "PricingTaskDef")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        taskDef.addContainer("ServerContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("example-server:0.1.0-SNAPSHOT"))
                .portMappings(List.of(PortMapping.builder().containerPort(8080).build()))
                .environment(Map.of(
                        "PRICING_TABLE_NAME", pricingTable.getTableName(),
                        "AWS_REGION", this.getRegion()
                ))
                .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder().streamPrefix("pricing-server").build()))
                .build()
        );

        ordersTable.grantReadData(taskDef.getTaskRole());
        
        outboxDispatcher.getRole().addToPrincipalPolicy(
                PolicyStatement.Builder.create()
                        .actions(List.of("dynamodb.DeleteItem"))
                        .resources(List.of(ordersTable.getTableArn()))
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
