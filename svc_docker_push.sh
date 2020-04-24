#!/usr/bin/env bash

set -euox pipefail

docker tag audio-transcription-service:latest $CONTAINER_REGISTRY_ENDPOINT/audio-transcription-service:latest
docker push $CONTAINER_REGISTRY_ENDPOINT/audio-transcription-service:latest