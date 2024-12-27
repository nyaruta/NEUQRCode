#!/bin/bash
# A reproducible build of patched Elise tool

git clone https://github.com/neucn/elise
wget 'https://github.com/neucn/elise/pull/4.patch'
pushd elise || exit 1
git apply ../4.patch
rm ../4.patch
GOOS=android GOARCH=arm64 go build -o ./elise -ldflags "-w -s -X 'main.version=v0.3.0-neuqrcode'" ./cmd/elise/main.go
popd || exit 1
