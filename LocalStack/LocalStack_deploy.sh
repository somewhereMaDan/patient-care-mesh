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

# Test credentials

export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-ap-south-1}
EP=http://localhost:4566

# Build only the services passed as arguments; otherwise build all services.
# Example: ./deploy.sh auth-service patient-service

ALL="auth-service billing-service analytics-service patient-service api-gateway"
BUILD="${@:-$ALL}"
echo "Building images: $BUILD"
docker compose build $BUILD

# Generate a unique image tag with timestamp for this deployment.
# The tag is included in the synthesized CloudFormation template,
# so ECS sees each deployment as a new image version.

export IMAGE_TAG=$(date +%s)
echo "$IMAGE_TAG" > .image_tag

# The CDK puts that tag in the template, so every service starts from a name Floci has never seen.
# (It's the image tag that changes, not the container name)

# Tag each service image with the deployment-specific image tag.
for svc in $ALL; do
  docker tag "patient-care-mesh-$svc" "$svc:$IMAGE_TAG"
done

# Synthesize the CDK stack into a CloudFormation template.
echo "Synthesizing template..."
mvn -q compile exec:java -Dexec.mainClass=com.pm.stack.LocalStack

# Remove the existing stack so ECS/RDS and other resources are recreated
# from the freshly synthesized template.
echo "Deleting old stack..."
aws --endpoint-url=$EP cloudformation delete-stack --stack-name patient-management
aws --endpoint-url=$EP cloudformation wait stack-delete-complete --stack-name patient-management

# Deploy the newly synthesized CloudFormation template to Floci
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

# Get the load balancer DNS name so the API Gateway can be accessed externally.
aws --endpoint-url=$EP elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text