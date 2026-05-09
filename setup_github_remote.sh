#!/usr/bin/env bash
set -euo pipefail

# Configure an `origin` remote using environment variables and verify push access.
# Supported env vars:
#   GITHUB_REPOSITORY="owner/repo" (preferred)
#   GITHUB_REMOTE_URL="https://github.com/owner/repo.git"
#   GITHUB_TOKEN or GH_TOKEN (optional, for authenticated dry-run push)

if [[ "${1:-}" == "--help" ]]; then
  cat <<'HELP'
Usage: ./setup_github_remote.sh

Environment variables:
  GITHUB_REPOSITORY   owner/repo format (preferred)
  GITHUB_REMOTE_URL   full remote URL (https://github.com/owner/repo.git)
  GITHUB_TOKEN        GitHub token with repo push access (optional for setup, required for push test)
  GH_TOKEN            Fallback token if GITHUB_TOKEN is not set

What this script does:
  1) Builds or reads the GitHub remote URL.
  2) Sets/updates git remote 'origin'.
  3) If a token is present, performs `git push --dry-run origin <current-branch>`.
HELP
  exit 0
fi

repo="${GITHUB_REPOSITORY:-}"
remote_url="${GITHUB_REMOTE_URL:-}"

if [[ -z "$remote_url" ]]; then
  if [[ -z "$repo" ]]; then
    echo "Error: set GITHUB_REPOSITORY (owner/repo) or GITHUB_REMOTE_URL." >&2
    exit 1
  fi
  remote_url="https://github.com/${repo}.git"
fi

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$remote_url"
  echo "Updated origin -> $remote_url"
else
  git remote add origin "$remote_url"
  echo "Added origin -> $remote_url"
fi

token="${GITHUB_TOKEN:-${GH_TOKEN:-}}"
branch="$(git branch --show-current)"

if [[ -z "$token" ]]; then
  echo "No token detected (GITHUB_TOKEN/GH_TOKEN). Skipping dry-run push test."
  echo "Remote configured successfully."
  exit 0
fi

# Authenticate only for this command by embedding token in remote URL.
auth_url="${remote_url/https:\/\/github.com/https:\/\/x-access-token:${token}@github.com}"

echo "Testing authenticated push permissions with dry-run on branch '$branch'..."
git push --dry-run "$auth_url" "$branch"
echo "Dry-run push succeeded. GitHub release/push permissions look good."
