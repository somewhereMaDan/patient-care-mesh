#!/bin/bash

set -e # stops the script if any commands fails

aws --endpoint-url=http://localhost:4566 cloudformation delete-stack \
    --stack-name patient-management

aws --endpoint-url=http://localhost:4566 cloudformation wait stack-delete-complete \
    --stack-name patient-management

aws --endpoint-url=http://localhost:4566 cloudformation deploy \
    --stack-name patient-management \
    --template-file "./cdk.out/localstack.template.json"

aws --endpoint-url=http://localhost:4566 cloudformation describe-stack-events \
    --stack-name patient-management \
    --query "StackEvents[?ResourceStatus=='CREATE_FAILED'].[LogicalResourceId,ResourceStatusReason]" \
    --output text
    
aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text