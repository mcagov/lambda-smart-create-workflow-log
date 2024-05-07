#!/bin/bash

#
# copy cert files form ssm to local nginx ssl folder
#

FULL_PATH_TO_SCRIPT="$(realpath "$0")"
SCRIPT_DIRECTORY="$(dirname "$FULL_PATH_TO_SCRIPT")"
REDIS_CERTS_DIR="./src/test/resources/redis/ssl"

mkdir -p "${REDIS_CERTS_DIR}"
rm -rf -v "${REDIS_CERTS_DIR}/*"

function saveSsmValue {
  ssm_path="$1"
  output_path="$2"
  aws ssm get-parameter --name "${ssm_path}" --region eu-west-2 --query "Parameter.Value" --output text > "${output_path}"
  echo "saved ${output_path}"
}

docker compose down
saveSsmValue "/local/tls/fullchain" "${REDIS_CERTS_DIR}/service.local.smart.mcga.uk-fullchain.pem"
saveSsmValue "/local/tls/key" "${REDIS_CERTS_DIR}/service.local.smart.mcga.uk-key.pem"
saveSsmValue "/local/tls/ca" "${REDIS_CERTS_DIR}/service.local.smart.mcga.uk-root-ca.pem"