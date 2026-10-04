# #!/bin/bash

# set -e # stops the script if any commands fails

# aws --endpoint-url=http://localhost:4566 cloudformation delete-stack \
#     --stack-name patient-management

# aws --endpoint-url=http://localhost:4566 cloudformation wait stack-delete-complete \
#     --stack-name patient-management

# aws --endpoint-url=http://localhost:4566 cloudformation deploy \
#     --stack-name patient-management \
#     --template-file "./cdk.out/localstack.template.json" || {
#     echo "Deploy failed. Failed resources:"
#     aws --endpoint-url=http://localhost:4566 cloudformation describe-stack-events \
#         --stack-name patient-management \
#         --query "StackEvents[?ResourceStatus=='CREATE_FAILED'].[LogicalResourceId,ResourceStatusReason]" \
#         --output text
#     exit 1
# }
    
# aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
#     --query "LoadBalancers[0].DNSName" --output text

#!/bin/bash
set -e
cd "$(dirname "$0")"

export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-ap-south-1}
EP=http://localhost:4566

# echo "Building images..."
# docker compose build
# for svc in auth-service billing-service analytics-service patient-service api-gateway; do
#   docker tag "patient-care-mesh-$svc" "$svc"
# done

ALL="auth-service billing-service analytics-service patient-service api-gateway"
BUILD="${@:-$ALL}"
echo "Building images: $BUILD"
(cd .. && docker compose build $BUILD)

export IMAGE_TAG=$(date +%s)
echo "$IMAGE_TAG" > .image_tag
for svc in $ALL; do
  docker tag "patient-care-mesh-$svc" "$svc:$IMAGE_TAG"
done

echo "Synthesizing template..."
mvn -q compile exec:java -Dexec.mainClass=com.pm.stack.LocalStack

echo "Deleting old stack..."
aws --endpoint-url=$EP cloudformation delete-stack --stack-name patient-management
aws --endpoint-url=$EP cloudformation wait stack-delete-complete --stack-name patient-management

echo "Deploying..."
aws --endpoint-url=$EP cloudformation deploy \
    --stack-name patient-management \
    --template-file "./cdk.out/localstack.template.json" || {
  echo "Deploy failed. Failed resources:"
  aws --endpoint-url=$EP cloudformation describe-stack-events \
      --stack-name patient-management \
      --query "StackEvents[?ResourceStatus=='CREATE_FAILED'].[LogicalResourceId,ResourceStatusReason]" \
      --output text
  exit 1
}

aws --endpoint-url=$EP elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text