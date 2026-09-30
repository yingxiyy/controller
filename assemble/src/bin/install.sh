#!/usr/bin/env bash

##############################################################################################
#
#  Software will be installed to the directory where the tar package is extracted, by default.
#
##############################################################################################

ENV_CONFIG="env.config"
DEPLOY_DIR=$(dirname $(realpath $0))


function echo_error {
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
echo "${TIMESTAMP} ERROR: $1"
}

function echo_info {
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
echo "${TIMESTAMP} INFO: $1"
}

function source_env {
current_dir=$(dirname $0)
if test -f ${DEPLOY_DIR}/${ENV_CONFIG} ; then
  set -a
  . ${DEPLOY_DIR}/${ENV_CONFIG}
else
  echo_error "Failed to get config file ${DEPLOY_DIR}/${ENV_CONFIG}, exit installation."
  exit 1
fi
}

function empty_check {
if [ "x$2" = "x" ]; then
  echo_error "The $1 is not set, please set it in ${ENV_CONFIG}. Exit installation."
  exit 1
fi
}


function validate_env {
empty_check "myIp" ${myIp}
empty_check "KAFKA_BROKER" ${KAFKA_BROKER}
empty_check "MONGO_SERVERS" ${MONGO_SERVERS}
empty_check "MONGO_DATABASE" ${MONGO_DATABASE}
empty_check "pmcZooKeepers" ${pmcZooKeepers}
}

function set_env {
source_env
validate_env
}

function install {
mkdir -p ${ROOT_DIR}
for entry in ${DEPLOY_DIR}/*-bin.tar.gz
do
  installedFolder=$(tar -tf ${entry} | head -1 | cut -f1 -d"/")
  tar -xvzf ${entry} -C ${ROOT_DIR} --no-same-owner
  ${ROOT_DIR}/${installedFolder}/bin/install.sh
done
}



#main
set_env
install

echo_info "Installation is completed."