# GitHub remote setup helper

This repository includes a helper script to configure `origin` and verify push permissions with a dry-run.

## Quick start

```bash
export GITHUB_REPOSITORY="OWNER/REPO"
export GITHUB_TOKEN="<your token>"  # or GH_TOKEN
./setup_github_remote.sh
```

You can also pass a full remote URL:

```bash
export GITHUB_REMOTE_URL="https://github.com/OWNER/REPO.git"
./setup_github_remote.sh
```

## Security note

- **Do not commit real GitHub tokens to this repository or any tracked file.**
- Keep secrets in environment variables or your CI/provider secret manager.

## Notes

- The script only performs `git push --dry-run`; it does not create or publish a GitHub Release.
- If no token is set, it still configures `origin` and skips permission testing.
