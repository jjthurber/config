#!/usr/bin/env bash

#
# Copyright 2022, TeamDev. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Redistribution and use in source and/or binary forms, with or without
# modification, must retain the above copyright notice and the following
# disclaimer.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
PRIMARY_MARK="*"
USAGE_TEMPLATE="Usage: publish.sh repositoryUrl='' tags='x,${PRIMARY_MARK}y,z' paths='foo,bar/baz'"
USAGE_EXAMPLE="Example: publish.sh repositoryUrl='https://github.com/SpineEventEngine/base.git' tags='v1.7.0,${PRIMARY_MARK}v1.8.0' paths='base,tools/proto-js-plugin'"

# Check that exactly three parameters were provided.
if [ "$#" -ne 3 ]; then
    echo "$USAGE_TEMPLATE"
    echo "$USAGE_EXAMPLE"
    exit 22 # Invalid argument
fi

# Declare variables matching those passed to the script.
for arg in "$@"
do
   key=$(echo "$arg" | cut -f1 -d=)

   key_length=${#key}
   value="${arg:key_length+1}"

   declare "$key"="$value"
done

# Check that all necessary for the script parameters were set.
if [ -z "$repositoryUrl" ] || [ -z "$tags" ] || [ -z "$paths" ]; then
    echo "$USAGE_TEMPLATE"
    echo "$USAGE_EXAMPLE"
    exit 22 # Invalid argument
fi

mkdir "workspace" && cd "workspace" || exit 2 # Folder does not exist.
git clone "$repositoryUrl" "."

# Create the `gh-pages` branch if it does not exist.
if ! [[ $(git branch --list --all origin/gh-pages) ]]; then
  git switch --orphan gh-pages
  git commit --allow-empty -m "Initial commit"
  git push -u origin gh-pages
fi

log() {
  echo "-----------------$1-----------------"
}

for tag in $(echo "$tags" | tr "," "\n")
do
  if [[ $tag == "$PRIMARY_MARK"* ]]; then
    # Remove the leading $PRIMARY_MARK.
    tag=${tag:1}
    is_primary=true
  else
    is_primary=false
  fi

  log "Started working on the \`$tag\` tag."
  git checkout -f "tags/$tag"
  git submodule update --init --recursive

  # Remove the leading 'v' to derive the version.
  version=${tag:1}

  jenv local 1.8

  # The version that will show up in Dokka-generated documentation.
  echo "val versionToPublish: String by extra(\"$version\")" >> "../version.gradle.kts"

  for path in $(echo "$paths" | tr "," "\n")
  do
      # Extracts the module name from a path. A path should use the "/" as a file separator.
      module="${path##*\/}"
      log "Started working on the \`$module\` module."

      ./gradlew ":$module:classes"
      mkdir "../$module"
      cp -r "$path/" "../$module/"

      cd ..
      echo "include(\"$module\")" >> "settings.gradle.kts"

      # Configuration in module's build files is not needed for the `classes` task,
      # but if present can result in an error, so it is removed completely.
      echo "" > "$module/build.gradle"
      echo "" > "$module/build.gradle.kts"

      ./gradlew ":$module:dokkaHtml"

      cd "workspace" || exit 2 # Folder does not exist.
  done

  git switch gh-pages
  git clean -fdx

  for path in $(echo "$paths" | tr "," "\n")
  do
    # Extracts the module name from a path. A path should use the "/" as a file separator.
    module="${path##*\/}"

    mkdir -p "dokka-reference/$module/v/$version"
    cp -r "../$module/build/docs/dokka/" "dokka-reference/$module/v/$version/"

    commit_message="Publish Dokka documentation for \`$module\` of \`$version\` "
    if [ $is_primary = true ]; then
      cp -r "../$module/build/docs/dokka/" "dokka-reference/$module/"
      commit_message+="as primary"
    else
      commit_message+="as secondary"
    fi

    git add "dokka-reference/$module/*"
    git commit -m "$commit_message"

    rm -rf "../$module"
    log "Finished working on the \`$module\` module."
  done

  git push

  rm "../version.gradle.kts"
  rm "../settings.gradle.kts"

  log "Finished working on the \`$tag\` tag."
done

cd ..
rm -rf "workspace"
rm -rf ".gradle"
