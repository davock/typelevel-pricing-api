#!/usr/bin/env bash


set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
ENDPOINT="${AWS_ENDPOINT_URL:-http://localhost:4566}"
ACCOUNT="000000000000"

echo "==> Checking for cdklocal"
if ! command -v cdklocal >/dev/null 2>&1; then
  echo "cdklocal not found. Install it with:"
  echo "  npm install -g aws-cdk aws-cdk-local"
  exit 1
fi
cdklocal --version

echo "==> Checking LocalStack is up at ${ENDPOINT}"
if ! curl -sf "${ENDPOINT}/_localstack/health" >/dev/null; then
  echo "LocalStack doesn't seem to be running. Starting it via docker-compose..."
  docker compose up -d localstack
  echo "Waiting for it to become healthy..."
  until curl -sf "${ENDPOINT}/_localstack/health" >/dev/null; do
    sleep 1
  done
fi
echo "LocalStack is up."

echo "==> Bootstrapping CDK toolkit stack into LocalStack (account ${ACCOUNT}, region ${REGION})"
cd "$(dirname "$0")/"
cdklocal bootstrap "aws://${ACCOUNT}/${REGION}"

cat <<EOT

Done. From modules/cdk you can now run, e.g.:

  sbt server/Docker/stage   # (from the project root) build the image the stack packages
  cdklocal synth  --app "sbt -error 'cdk/runMain com.example.cdk.CdkApp'"
  cdklocal deploy --app "sbt -error 'cdk/runMain com.example.cdk.CdkApp'"
  cdklocal destroy --app "sbt -error 'cdk/runMain com.example.cdk.CdkApp'"
EOT
