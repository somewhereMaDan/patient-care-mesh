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
TAG=$(cat "$(dirname "$0")/.image_tag")
check_image() {  # $1 = container name fragment, $2 = image name
  c=$(docker ps -qf "name=$1")
  running=$(docker inspect -f '{{.Image}}' "$c")
  built=$(docker image inspect -f '{{.Id}}' "$2:$TAG")
  if [ "$running" = "$built" ]; then echo "  ok: $2"; else echo "  STALE: $2 is not running the latest image"; fi
}
check_image "-auth-serviceContainer" auth-service
check_image "-patient-serviceContainer" patient-service
check_image "-billing-serviceContainer" billing-service
check_image "-analytics-serviceContainer" analytics-service
check_image "APIGatewayContainer" api-gateway

echo "Done."

echo "Finding auth DB..."
AUTH_DB=""
for c in $(docker ps -qf name=floci-rds); do
  if docker exec -e PGPASSWORD=localpassword "$c" psql -U admin_user -h localhost -d auth-service-db -tAc "select 1" >/dev/null 2>&1; then
    AUTH_DB=$c
  fi
done
[ -n "$AUTH_DB" ] || { echo "auth DB not found"; exit 1; }

echo "Waiting for users table..."
for i in $(seq 40); do
  docker exec -e PGPASSWORD=localpassword "$AUTH_DB" psql -U admin_user -h localhost -d auth-service-db -tAc "select to_regclass('public.users')" | grep -q users && break
  sleep 3
done

count=$(docker exec -e PGPASSWORD=localpassword "$AUTH_DB" psql -U admin_user -h localhost -d auth-service-db -tAc "select count(*) from users")
if [ "$count" = "0" ]; then
  echo "Seeding test user from old volume..."
  docker rm -f old-auth-db >/dev/null 2>&1 || true
  docker run -d --name old-auth-db \
    -v patient-care-mesh_auth_db_data:/var/lib/postgresql/data \
    -e POSTGRES_PASSWORD=password postgres:17 >/dev/null
  until docker exec old-auth-db pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done
  sleep 3
  docker exec old-auth-db pg_dump -U postgres -d db --data-only --table=users --column-inserts \
    | sed '/^\\/d' \
    | docker exec -i -e PGPASSWORD=localpassword "$AUTH_DB" psql -U admin_user -h localhost -d auth-service-db
  docker rm -f old-auth-db >/dev/null
else
  echo "Users already present, skipping seed."
fi
echo "Done."