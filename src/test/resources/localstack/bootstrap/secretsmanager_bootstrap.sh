#!/usr/bin/env bash

set -euo pipefail

# enable debug
# set -x

echo "configuring secretsmanager"
echo "==================="
AWS_REGION=${DEFAULT_REGION:=eu-west-2}

create_secret() {
    local SECRET_NAME=$1
    local SECRET_VALUE=$2
    echo  "awslocal secretsmanager create-secret \
            --name \"${SECRET_NAME}\" \
            --secret-string \"${SECRET_VALUE}\""
    awslocal secretsmanager create-secret \
            --name "${SECRET_NAME}" \
            --secret-string "${SECRET_VALUE}"
}

create_secret "${OKTA_CLIENT_ID_KEY}" "${OKTA_CLIENT_ID}"
create_secret "${OKTA_CLIENT_SECRET_KEY}" "${OKTA_CLIENT_SECRET}"
create_secret "${REDIS_PASSWORD_KEY}" "${REDIS_PASSWORD}"

