# Releasing

This document describes the versioning scheme, release process, and hotfix workflow for ZAC.

## Versioning scheme

ZAC follows [Semantic Versioning](https://semver.org/) with these conventions:

| Version | Meaning | Example |
|---------|---------|---------|
| `X.Y.0` | Sprint release (minor or major bump) | `4.7.0`, `5.0.0` |
| `X.Y.Z` (Z > 0) | Hotfix on a previous release | `4.6.1`, `4.6.2` |
| `X.Y.0-dev` | Development pre-release (rolling, updated on every push to main) | `4.7.0-dev` |
| `X.Y.0-dev.N` | Pinned development build (N = GitHub Actions run number) | `4.7.0-dev.123` |

## Development builds

Every push to the `main` branch automatically:

1. Calculates the next version by taking the latest release and bumping the minor version (e.g., `4.6.0` → `4.7.0-dev`)
2. Builds, tests, and pushes a Docker image with tags:
   - `latest` — always points to the most recent main build
   - `X.Y.0-dev` — rolling dev tag, overwritten each build
   - `X.Y.0-dev.N` — pinned tag with the build number for traceability
3. Creates (or updates) a single **pre-release** on GitHub named `vX.Y.0-dev`
   - This pre-release is replaced on every push — there is only ever one dev pre-release visible

No permanent git tags are created for development builds.

## Sprint releases

At the end of a sprint, a release is cut by manually dispatching the **Build, test & deploy** workflow on the `main` branch:

1. Go to **Actions** → **Build, test & deploy** → **Run workflow**
2. Select the `main` branch
3. Choose the version bump: **minor** (default, e.g., `4.6.0` → `4.7.0`) or **major** (e.g., `4.6.0` → `5.0.0`)
4. Click **Run workflow**

The workflow will:
- Delete the existing `vX.Y.0-dev` pre-release and its tag
- Build, test, and push a Docker image with tags: `X.Y.0`, `X.Y`, `X`, and `latest`
- Create a proper **GitHub Release** tagged `vX.Y.0`, marked as **Latest**
- Trigger automatic provisioning (if enabled)

### After a sprint release

Subsequent development builds will automatically target the next minor version (e.g., `4.8.0-dev`).

## Hotfixes

Hotfixes are for critical fixes to a previously released version. They use proper patch versions (e.g., `4.6.1`).

### Creating a hotfix

1. Create a branch from the release tag:
   ```bash
   git checkout -b hotfix/v4.6.1 v4.6.0
   ```
2. Apply your fix and push the branch
3. Go to **Actions** → **Build, test & deploy** → **Run workflow**
4. Select the `hotfix/v4.6.1` branch
5. Enter the hotfix version in the **hotfix_version** field (e.g., `4.6.1`)
6. Click **Run workflow**

The workflow will:
- Validate the version is a valid semver and the tag does not already exist
- Build, test, and push a Docker image tagged `4.6.1`
- Create a **GitHub Release** tagged `v4.6.1` (not marked as Latest, since `main` is ahead)

> **Note:** Hotfix Docker images are intentionally **not** tagged with `latest`, `X`, or `X.Y` to avoid overwriting the current release's tags.

## Docker image tags

| Scenario | Docker tags |
|----------|-------------|
| Development build | `latest`, `X.Y.0-dev`, `X.Y.0-dev.N` |
| Sprint release | `latest`, `X.Y.0`, `X.Y`, `X` |
| Hotfix | `X.Y.Z` only |

All images are published to `ghcr.io/infonl/zaakafhandelcomponent`.

## GitHub Releases page

The releases page will typically look like:

```
vX.Y.0-dev          ← Pre-release (rolling development build)
v4.7.0              ← Latest Release (current sprint)
v4.6.1              ← Hotfix
v4.6.0              ← Previous sprint release
```

## Docker image cleanup

A nightly workflow automatically cleans up old Docker images while preserving:
- `latest`
- Minor version tags (e.g., `4.7`)
- All patch version tags (e.g., `4.7.0`, `4.6.1`)
- Tags with a simple suffix (e.g., `4.7.0-dev`, but not pinned build tags like `4.7.0-dev.123`)

Build artifacts like `main-12345` are eligible for cleanup beyond the 10 most recent.
