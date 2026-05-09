#!/usr/bin/env bash
set -euo pipefail

# Create a GitHub release with built jars using the REST API.
# Requires: curl, python3, git, built jars from `./gradlew clean build`
# Env:
#   GITHUB_REPOSITORY=owner/repo
#   GITHUB_TOKEN or GH_TOKEN
# Optional args:
#   --tag v1.11
#   --name "ChatDelayMod v1.11"
#   --repo owner/repo (overrides GITHUB_REPOSITORY)

TAG=""
NAME=""
REPO_ARG=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag)
      TAG="${2:-}"
      shift 2
      ;;
    --name)
      NAME="${2:-}"
      shift 2
      ;;
    --repo)
      REPO_ARG="${2:-}"
      shift 2
      ;;
    --help|-h)
      cat <<'HELP'
Usage: ./scripts/create-github-release.sh [--tag vX.Y.Z] [--name "Release Name"] [--repo owner/repo]

Environment variables:
  GITHUB_REPOSITORY   owner/repo format (required)
  GITHUB_TOKEN        token with repo contents:write
  GH_TOKEN            fallback token if GITHUB_TOKEN is unset

If --tag is omitted, tag is auto-derived as: v<mod_version from gradle.properties>.
HELP
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

REPO="${GITHUB_REPOSITORY:-}"
if [[ -n "$REPO_ARG" ]]; then
  REPO="$REPO_ARG"
fi
TOKEN="${GITHUB_TOKEN:-${GH_TOKEN:-}}"

if [[ -z "$REPO" ]]; then
  echo "Error: set GITHUB_REPOSITORY=owner/repo" >&2
  exit 1
fi
if [[ -z "$TOKEN" ]]; then
  echo "Error: set GITHUB_TOKEN or GH_TOKEN" >&2
  exit 1
fi

if [[ -z "$TAG" ]]; then
  VERSION="$(awk -F= '/^mod_version=/{print $2}' gradle.properties | tr -d '[:space:]')"
  TAG="v${VERSION}"
fi
if [[ -z "$NAME" ]]; then
  NAME="ChatDelayMod ${TAG}"
fi

mapfile -t LIB_JARS < <(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-sources.jar" | sort)
mapfile -t SOURCE_JARS < <(find build/libs -maxdepth 1 -type f -name "*-sources.jar" | sort)
if [[ ${#LIB_JARS[@]} -eq 0 ]]; then
  echo "Error: expected Fabric runtime jar in build/libs. Run ./gradlew clean build first." >&2
  exit 1
fi
LIB_JAR="${LIB_JARS[0]}"

./scripts/export-jar.sh >/dev/null
mapfile -t DOWNLOAD_JARS < <(find build/download -maxdepth 1 -type f -name "*.jar" | sort)
if [[ ${#DOWNLOAD_JARS[@]} -eq 0 ]]; then
  echo "Error: expected exported jar in build/download. Run ./scripts/export-jar.sh." >&2
  exit 1
fi
DOWNLOAD_JAR="${DOWNLOAD_JARS[0]}"

UPLOAD_JARS=("$LIB_JAR")
if [[ ${#SOURCE_JARS[@]} -gt 0 ]]; then
  UPLOAD_JARS+=("${SOURCE_JARS[0]}")
fi
if [[ "$(basename "$DOWNLOAD_JAR")" != "$(basename "$LIB_JAR")" ]]; then
  UPLOAD_JARS+=("$DOWNLOAD_JAR")
fi

api_base="https://api.github.com/repos/${REPO}"

if ! git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null; then
  git tag "$TAG"
fi

remote_url="$(git remote get-url origin 2>/dev/null || true)"
if [[ -z "$remote_url" ]]; then
  remote_url="https://github.com/${REPO}.git"
fi

if [[ "$remote_url" == https://github.com/* ]]; then
  auth_push_url="${remote_url/https:\/\/github.com/https:\/\/x-access-token:${TOKEN}@github.com}"
  if git ls-remote --exit-code --tags "$auth_push_url" "refs/tags/${TAG}" >/dev/null 2>&1; then
    echo "Tag ${TAG} already exists on remote. Skipping tag push."
  else
    git push "$auth_push_url" "$TAG"
  fi
else
  if git ls-remote --exit-code --tags origin "refs/tags/${TAG}" >/dev/null 2>&1; then
    echo "Tag ${TAG} already exists on remote. Skipping tag push."
  else
    git push origin "$TAG"
  fi
fi

release_json="$(env TAG="$TAG" NAME="$NAME" python3 - <<'PY'
import json, os
print(json.dumps({
  "tag_name": os.environ["TAG"],
  "name": os.environ["NAME"],
  "generate_release_notes": True,
  "draft": False,
  "prerelease": False
}))
PY
)"

fetch_release_by_tag() {
  local tag="$1"
  local out_file="$2"
  local http_code
  http_code="$(curl -sS -o "$out_file" -w "%{http_code}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "${api_base}/releases/tags/${tag}")"
  if [[ "$http_code" == "200" ]]; then
    return 0
  fi
  if [[ "$http_code" == "404" ]]; then
    return 1
  fi

  echo "Failed to fetch release for tag ${tag} (HTTP ${http_code})." >&2
  cat "$out_file" >&2
  exit 1
}

release_payload_file="$(mktemp)"
if fetch_release_by_tag "$TAG" "$release_payload_file"; then
  echo "Using existing release for tag ${TAG}."
else
  curl -sS --fail-with-body --http1.1 --retry 3 --retry-delay 2 -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "${api_base}/releases" \
    -d "$release_json" > "$release_payload_file"
  echo "Created release for tag ${TAG}."
fi

release_payload="$(cat "$release_payload_file")"
upload_url="$(printf '%s' "$release_payload" | python3 -c 'import sys,json; d=json.load(sys.stdin); print((d.get("upload_url") or "").split("{")[0])')"
release_html="$(printf '%s' "$release_payload" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("html_url", ""))')"
release_id="$(printf '%s' "$release_payload" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("id", ""))')"

if [[ -z "$release_id" ]] || [[ -z "$upload_url" ]]; then
  echo "Failed to resolve or create release. Response:" >&2
  printf '%s\n' "$release_payload" >&2
  exit 1
fi

for jar in "${UPLOAD_JARS[@]}"; do
  name="$(basename "$jar")"
  existing_asset_id="$(printf '%s' "$release_payload" | env ASSET_NAME="$name" python3 -c 'import json,sys,os; d=json.load(sys.stdin); n=os.environ["ASSET_NAME"]; a=next((x.get("id") for x in d.get("assets", []) if x.get("name")==n), ""); print(a if a is not None else "")')"
  if [[ -n "$existing_asset_id" ]]; then
    curl -sS --fail-with-body -X DELETE \
      -H "Authorization: Bearer ${TOKEN}" \
      -H "Accept: application/vnd.github+json" \
      "${api_base}/releases/assets/${existing_asset_id}" >/dev/null
    echo "Deleted existing asset: ${name}"
  fi

  upload_resp="$(curl -sS --fail-with-body --http1.1 --retry 3 --retry-delay 2 -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/java-archive" \
    --data-binary "@${jar}" \
    "${upload_url}?name=${name}")"

  uploaded_name="$(printf '%s' "$upload_resp" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("name", ""))')"
  if [[ "$uploaded_name" != "$name" ]]; then
    echo "Upload verification failed for ${name}. Response:" >&2
    printf '%s\n' "$upload_resp" >&2
    exit 1
  fi
  echo "Uploaded asset: ${name}"
done

final_release_payload_file="$(mktemp)"
fetch_release_by_tag "$TAG" "$final_release_payload_file" || {
  echo "Failed to fetch release for post-upload verification." >&2
  exit 1
}

expected_assets_file="$(mktemp)"
printf '%s\n' "${UPLOAD_JARS[@]##*/}" > "$expected_assets_file"
missing_assets="$(python3 - "$final_release_payload_file" "$expected_assets_file" <<'PY'
import json, sys
with open(sys.argv[1], 'r', encoding='utf-8') as fh:
    payload = json.load(fh)
with open(sys.argv[2], 'r', encoding='utf-8') as fh:
    expected = [line.strip() for line in fh if line.strip()]
assets = {a.get('name') for a in payload.get('assets', [])}
missing = [name for name in expected if name not in assets]
print(','.join(missing))
PY
)"

if [[ -n "$missing_assets" ]]; then
  echo "Release verification failed. Missing assets: ${missing_assets}" >&2
  exit 1
fi

echo "Release ready: ${release_html}"
