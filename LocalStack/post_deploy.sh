#!/bin/bash
set -e

EP=http://localhost:4566
EXPECTED_BROKERS="172.19.0.10:9092"

echo "Checking Kafka cluster..."
ARN=$(aws --endpoint-url=$EP kafka list-clusters --query "ClusterInfoList[0].ClusterArn" --output text)
if [ "$ARN" = "None" ]; then
  echo "Creating Kafka cluster..."
  aws --endpoint-url=$EP kafka create-cluster \
    --cluster-name kafka-cluster \
    --kafka-version "3.6.1" \
    --number-of-broker-nodes 1 \
    --broker-node-group-info '{"InstanceType":"kafka.m5.large","ClientSubnets":["subnet-1"]}' >/dev/null
  for i in $(seq 40); do
    state=$(aws --endpoint-url=$EP kafka list-clusters --query "ClusterInfoList[0].State" --output text)
    [ "$state" = "ACTIVE" ] && break
    sleep 3
  done
  ARN=$(aws --endpoint-url=$EP kafka list-clusters --query "ClusterInfoList[0].ClusterArn" --output text)
fi

BROKERS=$(aws --endpoint-url=$EP kafka get-bootstrap-brokers --cluster-arn "$ARN" --query BootstrapBrokerString --output text)
echo "Kafka brokers: $BROKERS"
if [ "$BROKERS" != "$EXPECTED_BROKERS" ]; then
  echo "WARNING: brokers differ from $EXPECTED_BROKERS in LocalStack.java."
  echo "Update SPRING_KAFKA_BOOTSTRAP_SERVERS there, then re-run mvn and the deploy."
fi

echo "Adding DNS aliases..."
for svc in auth-service patient-service billing-service analytics-service; do
  c=$(docker ps -qf "name=-${svc}Container")
  docker network disconnect floci-net "$c"
  docker network connect --alias "$svc.patient-management.local" floci-net "$c"
done


echo "Checking that containers run the latest images..."
check_image() {  # $1 = container name fragment, $2 = image tag
  c=$(docker ps -qf "name=$1")
  running=$(docker inspect -f '{{.Image}}' "$c")
  built=$(docker image inspect -f '{{.Id}}' "$2")
  if [ "$running" = "$built" ]; then echo "  ok: $2"; else echo "  STALE: $2 is not running the latest image"; fi
}
check_image "-auth-serviceContainer" auth-service
check_image "-patient-serviceContainer" patient-service
check_image "-billing-serviceContainer" billing-service
check_image "-analytics-serviceContainer" analytics-service
check_image "APIGatewayContainer" api-gateway

echo "Done."