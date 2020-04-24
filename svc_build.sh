#!/usr/bin/env bash

set -euxo pipefail

#mvn clean package -f app/
docker build . -t audio-transcription-service:latest