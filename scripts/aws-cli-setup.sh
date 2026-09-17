#!/usr/bin/env bash
# One-time setup of a named AWS CLI profile for this project, plus the
# CDK/LocalStack tooling used alongside it. Safe to re-run.
set -euo pipefail

PROFILE="${AWS_PROFILE:-example-dev}"
REGION="${AWS_REGION:-us-east-1}"

echo "==> Checking for aws-cli v2"
if ! command -v aws >/dev/null 2>&1; then
  echo "aws-cli not found. Install it first:"
  echo "  macOS:   brew install awscli"
  echo "  Linux:   curl -sSL https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip && unzip awscliv2.zip && sudo ./aws/install"
  exit 1
fi
aws --version

echo "==> Configuring profile '${PROFILE}' (real AWS credentials)"
aws configure --profile "${PROFILE}" <<EOF
$(read -p "AWS Access Key ID: " k; echo "$k")
$(read -sp "AWS Secret Access Key: " s; echo; echo "$s")
${REGION}
json
EOF

echo "==> Installing Node-based CDK tooling"
if ! command -v cdk >/dev/null 2>&1; then
  npm install -g aws-cdk
fi
if ! command -v cdklocal >/dev/null 2>&1; then
  npm install -g aws-cdk-local
fi

echo "==> Bootstrapping CDK in ${REGION} for profile ${PROFILE}"
echo "    (run this once per account/region before the first 'cdk deploy')"
echo "    cdk bootstrap aws://ACCOUNT_ID/${REGION} --profile ${PROFILE}"

cat <<'EOT'

Done. Common commands from here:

  # deploy the real stack
  cd modules/cdk && cdk deploy --profile example-dev

  # run tests / start the server locally
  sbt server/run

  # build + stage the docker image the CDK stack packages
  sbt server/Docker/stage

See scripts/localstack-setup.sh for the LocalStack-specific setup.
EOT
