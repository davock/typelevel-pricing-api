#!/usr/bin/env bash
# Configures a second AWS CLI profile ("localstack") pointed at a local
# LocalStack container, plus the awslocal/cdklocal wrappers.
set -euo pipefail

PROFILE="localstack"
REGION="${AWS_REGION:-us-east-1}"
ENDPOINT="http://localhost:4566"

echo "==> Starting LocalStack (docker-compose.yml)"
docker compose up -d localstack

echo "==> Waiting for LocalStack to be healthy"
until curl -sf "${ENDPOINT}/_localstack/health" >/dev/null; do
  sleep 1
done
echo "LocalStack is up at ${ENDPOINT}"

echo "==> Configuring AWS CLI profile '${PROFILE}' with dummy credentials"
# LocalStack doesn't validate credentials by default — any non-empty values work.
aws configure set aws_access_key_id     test --profile "${PROFILE}"
aws configure set aws_secret_access_key test --profile "${PROFILE}"
aws configure set region                "${REGION}" --profile "${PROFILE}"
aws configure set output                json --profile "${PROFILE}"

echo "==> Installing awslocal / cdklocal (thin wrappers that inject --endpoint-url)"
if ! command -v awslocal >/dev/null 2>&1; then
  pip install --user awscli-local
fi
if ! command -v cdklocal >/dev/null 2>&1; then
  npm install -g aws-cdk-local aws-cdk
fi

cat <<EOT

Done. Two equivalent ways to talk to LocalStack:

  1) awslocal (wraps aws-cli, always targets ${ENDPOINT}):
       awslocal s3 ls

  2) plain aws-cli with the '${PROFILE}' profile + explicit endpoint:
       aws --profile ${PROFILE} --endpoint-url ${ENDPOINT} s3 ls

For the app itself, point it at LocalStack via env vars consumed by
modules/server/.../config/AppConfig.scala:

  export AWS_REGION=${REGION}
  export AWS_ENDPOINT_URL=${ENDPOINT}

For CDK deploys against LocalStack, bootstrap once then deploy with cdklocal:

  cd modules/cdk
  cdklocal bootstrap aws://000000000000/${REGION}
  cdklocal deploy --app "sbt -error 'cdk/runMain com.example.cdk.CdkApp'"

Tear down with: docker compose down
EOT
