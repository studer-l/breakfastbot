#!/usr/bin/env sh
set -e
pushd ..

# extract version
version=$(grep -o 'let version = ".*"' flake.nix | awk '{print $4}' | tr -d '"')

# create docker image
nix build -L .#docker  && docker load < result

docker push studerl/breakfastbot:${version}
