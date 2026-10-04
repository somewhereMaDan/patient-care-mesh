package com.pm.stack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.BootstraplessSynthesizer;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Token;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.CloudMapOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

public class LocalStack extends Stack {
    private final Vpc vpc;
    private final Cluster ecsCluster;
    private static final String IMAGE_TAG = System.getenv().getOrDefault("IMAGE_TAG", "latest");

    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "auth-service-db");

        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patient-service-db");

        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(patientServiceDb,
                "PatientServiceDBHealthCheck");

        CfnCluster mskCluster = createMskCluster();

        this.ecsCluster = createEcsCluster();

        FargateService authService = createFargateService("AuthService",
                "auth-service",
                List.of(4005),
                authServiceDb,
                Map.of("JWT_SECRET", "Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM="));

        // authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);

        FargateService billingService = createFargateService("BillingService",
                "billing-service",
                List.of(4001, 9001),
                null,
                null);

        FargateService analyticsService = createFargateService("AnalyticsService",
                "analytics-service",
                List.of(4002),
                null,
                null);

        analyticsService.getNode().addDependency(mskCluster);

        FargateService patientService = createFargateService("PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "billing-service.patient-management.local",
                        "BILLING_SERVICE_GRPC_PORT", "9001"));
        patientService.getNode().addDependency(patientServiceDb);
        // patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);

        createApiGatewayService();
    }

    private Vpc createVpc() {
        return Vpc.Builder
                .create(this, "PatientManagementVPC")
                .vpcName("PatientManagementVPC")
                .maxAzs(2)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName) {
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                // .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .credentials(Credentials.fromPassword("admin_user",
                        SecretValue.unsafePlainText("localpassword")))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        // .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .port(Token.asNumber(5432))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(3)
                        .build())
                .build();
    }

    private CfnCluster createMskCluster() {
        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge")
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .brokerAzDistribution("DEFAULT")
                        .build())
                .build();
    }

    private Cluster createEcsCluster() {
        return Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())
                .build();
    }

    private FargateService createFargateService(String id,
            String imageName,
            List<Integer> ports,
            DatabaseInstance db,
            Map<String, String> additionalEnvVars) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, id + "Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                // .image(ContainerImage.fromRegistry(imageName))
                .image(ContainerImage.fromRegistry(imageName + ":" + IMAGE_TAG))
                .portMappings(ports.stream()
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                .logGroupName("/ecs/" + imageName)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix(imageName)
                        .build()));

        Map<String, String> envVars = new HashMap<>();
        // envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "172.19.0.10:9092");
        // envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS",
        //         "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        if (additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        if (db != null) {
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName));
            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            // envVars.put("SPRING_DATASOURCE_PASSWORD",
            // db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_DATASOURCE_PASSWORD", "localpassword");
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
                .cloudMapOptions(CloudMapOptions.builder().name(imageName).build())
                .build();
    }

    private void createApiGatewayService() {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, "APIGatewayTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions containerOptions = ContainerDefinitionOptions.builder()
                // .image(ContainerImage.fromRegistry("api-gateway"))
                .image(ContainerImage.fromRegistry("api-gateway:" + IMAGE_TAG))
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",

                        "AUTH_SERVICE_URL",
                        "http://auth-service.patient-management.local:4005/auth",
                        "AUTH_ROUTE_URI", "http://auth-service.patient-management.local:4005",

                        "PATIENT_ROUTE_URI",
                        "http://patient-service.patient-management.local:4000"))
                .portMappings(List.of(4004).stream()
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                .logGroupName("/ecs/api-gateway")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix("api-gateway")
                        .build()))
                .build();

        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService apiGateway = ApplicationLoadBalancedFargateService.Builder
                .create(this, "APIGatewayService")
                .cluster(ecsCluster)
                .serviceName("api-gateway")
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(60))
                .build();
    }

    public static void main(final String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "localstack", props);
        app.synth();
        System.out.println("App synthesizing in progress...");
    }
}


/* 
❯❯ Creating the KAFKA Cluster 
aws --endpoint-url=http://localhost:4566 kafka create-cluster \
  --cluster-name kafka-cluster \
  --kafka-version "3.6.1" \
  --number-of-broker-nodes 1 \
  --broker-node-group-info '{"InstanceType":"kafka.m5.large","ClientSubnets":["subnet-1"]}'

❯❯ To see the details of the cluster
 aws --endpoint-url=http://localhost:4566 kafka describe-cluster \
        --cluster-arn arn:aws:kafka:ap-south-1:000000000000:cluster/kafka-cluster/4c072c7d-3dab-4ecb-96f9-46b2b43d9080

To check which Docker network the Redpanda container joined
docker inspect -f '{{json .NetworkSettings.Networks}}' $(docker ps -qf ancestor=redpandadata/redpanda:latest) | head -c 600

❯❯ This starts Floci with a CUSTOM DOCKER NETWORK, so Floci and the containers it creates can COMMUNICATE with each other.
Docker's own DNS on floci-net can resolve a name if the container has a network alias with that name, which is what Compose does for you.

docker run -d --name floci --network floci-net \
-e FLOCI_SERVICES_ECS_DOCKER_NETWORK=floci-net \
-p 4566:4566 -p 80:80 \
-v /var/run/docker.sock:/var/run/docker.sock \
-u root floci/floci:latest

        floci-net
          │
┌─────────┼─────────┐
│         │         │
Floci API Gateway Auth Service
│
Patient Service

❯❯ Adding the ALIASES to the running containers
for svc in auth-service patient-service billing-service analytics-service; do
  c=$(docker ps -qf "name=-${svc}Container")
  docker network disconnect floci-net "$c"
  docker network connect --alias "$svc.patient-management.local" floci-net "$c"
done

--> so can we get DNS names like - auth-service.patient-management.local,....


with Floci, if its CloudFormation support for MSK doesn't actually provision the underlying Kafka cluster, you can end up with:

                  Floci
                    │
          ┌─────────┴─────────┐
          ↓                   ↓
      Cloud Map              MSK
          │                   │
 CloudFormation           CloudFormation
   doesn't create          may not create
   actual resource         actual cluster
          │                   │
          ↓                   ↓
     ❌ DNS names          ❌ Kafka
          │                   │
          ↓                   ↓
   workaround:           workaround:
   Docker aliases        AWS CLI create-cluster

*/
