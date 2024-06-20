#!/bin/bash

#--------------Environment variables------------------#
RELEASE_VERSION=$1
PLUGIN_ARTIFACT_NAME=$2
PLUGIN_REPOSITORY_NAME=$3
RELEASES_FILE="releases.json"
#-----------------------------------------------------#

ARTIFACT_URLS_FILE=".github/artifact_urls.temp"

get_current_version_details() {
    jq --arg version "$RELEASE_VERSION" '.[] | select(.version == ($version | tonumber))' "$RELEASES_FILE"
}

get_dependencies() {
    echo "$1" | jq -r '.dependencies[]'
}

get_artifacts_urls() {
    gh release view -R Blazemeter/$PLUGIN_REPOSITORY_NAME \
        "v$RELEASE_VERSION" --json assets -q '.assets.[].url' > "$ARTIFACT_URLS_FILE"
}

find_url_for_dependency() {
    grep -F "$1" "$ARTIFACT_URLS_FILE"
}

build_libs() {
    get_artifacts_urls
    local dependencies="$1"
    local undera_dependencies="{}"

    while IFS= read -r dependency; do
        name=$(echo "$dependency" | cut -d'>' -f1)
        version=$(echo "$dependency" | cut -d'=' -f2)
        search_result=$(find_url_for_dependency "$name")

        if [ -n "$search_result" ]; then
            undera_dependencies=$(echo "$undera_dependencies" | jq --arg dep "$dependency" --arg url "$search_result" '. + {($dep): $url}')
        else
            echo "Dependency $name>=$version not found in artifacts download url"
        fi
    done <<< "$dependencies"

    echo "$undera_dependencies"
}

create_release_object() {
    local version="$1"
    local what_is_new="$2"
    local plugin_url="$3"
    local dependencies="$4"
    jq -n --arg version "$version" --arg what_is_new "$what_is_new" \
        --arg plugin_url "$plugin_url" --argjson libs "$dependencies" \
        '{($version): {changes: $what_is_new, downloadUrl: $plugin_url, libs: $libs, "depends": ["bzm-repositories"]}}'
}


current_version_details=$(get_current_version_details)
current_version_dependencies=$(get_dependencies "$current_version_details")

libs=$(build_libs "$current_version_dependencies")

plugin_url=$(find_url_for_dependency "$PLUGIN_ARTIFACT_NAME")
what_is_new=$(echo "$current_version_details" | jq -r '.what_is_new')

release_object=$(create_release_object "$RELEASE_VERSION" "$what_is_new" "$plugin_url" "$libs")
rm -rf $ARTIFACT_URLS_FILE
echo "$release_object" | jq -c '.'
