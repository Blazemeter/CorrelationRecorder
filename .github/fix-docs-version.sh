#!/bin/bash
#
# This script takes care of setting proper version in docs.
#
set -eo pipefail

VERSION=$1

update_file_versions() {
  local VERSION="$1"
  local FILE="$2"
  sed -i "s/<version>.*<\/version>/<version>${VERSION}<\/version>/g" README.md
}

update_file_versions ${VERSION} README.md

git add README.md
git config --local user.email "$(git log --format='%ae' HEAD^!)"
git config --local user.name "$(git log --format='%an' HEAD^!)"
git commit -m "[skip ci] Updated README version"
git push origin HEAD:master
