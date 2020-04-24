#!/usr/bin/env bash

set -euox pipefail

docker login -u $CONTAINER_REGISTRY_USERNAME -p $CONTAINER_REGISTRY_PASSWORD $CONTAINER_REGISTRY_ENDPOINT

docker tag audio-transcription-service:latest $CONTAINER_REGISTRY_ENDPOINT/audio-transcription-service:latest
docker push $CONTAINER_REGISTRY_ENDPOINT/audio-transcription-service:latest