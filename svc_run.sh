#!/usr/bin/env bash

set -euox pipefail


DOCKER_ENV_FLAGS=$(grep . .envrc.template.service | cut -d ' ' -f 2 | cut -d '=' -f 1 | xargs -L 1 -I{} echo " -e" {} | tr -d '\n' | sed -e 's/^[[:space:]]*//')
DOCKER_PORT_FLAGS="-p 8080:8080"

docker run $DOCKER_PORT_FLAGS $DOCKER_ENV_FLAGS audio-transcription-service:latest
