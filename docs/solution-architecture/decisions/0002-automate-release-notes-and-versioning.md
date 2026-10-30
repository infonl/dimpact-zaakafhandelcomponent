---
status: accepted
date: 2025-05-27
decision-makers: Team Genève
tags: [release, ci]
---

# Automate release notes and versioning

## Context and Problem Statement

PodiumD requires consistent and complete release notes for every released version of ZAC. Creating versions and
release notes by hand is recurring, error-prone work. How do we decide when a release is created and which version
it gets? See [PZ-607](https://dimpact.atlassian.net/browse/PZ-607).

## Decision Drivers

* Consistent and complete release notes for every ZAC version
* Clear communication of what a new ZAC version contains and how it differs from the previous one
* Less manual, error-prone work
* A release at the end of every sprint, and a hotfix release of an older version when needed

## Considered Options

* Commit message annotations that indicate the version bump
* A Git tag that starts the release
* A release branch that starts the release
* A release on every build of the main branch
* A manually started release workflow on the main branch, with a choice of version bump

## Decision Outcome

Chosen option: "A manually started release workflow on the main branch, with a choice of version bump", combined
with an automated development pre-release on every build of the main branch. Versions follow
[Semantic Versioning](https://semver.org/) and are derived from the highest `vMAJOR.MINOR.PATCH` Git tag.

* Every push to the main branch builds a development version `MAJOR.(MINOR + 1).0-dev`. The Docker image is tagged
  `latest`, with this version and with this version plus the build number. The GitHub pre-release of this version is
  updated, and older development pre-releases are deleted.
* A sprint release is started by hand by running the build workflow on the main branch, with the choice of a `minor`
  or `major` version bump. The Docker image is tagged `latest`, with the version, `MAJOR` and `MAJOR.MINOR`, and a GitHub
  release is created and marked as latest.
* A hotfix release is started by hand by running the build workflow on a `hotfix/*` branch, with an explicit
  `MAJOR.MINOR.PATCH` version that does not exist yet. The Docker image is tagged with this version only, and its
  GitHub release is not marked as latest.
* GitHub generates the release notes from the pull requests merged since the previous release tag. Every release also
  lists the component versions from `DEPENDENCIES.md` that ZAC was tested against.

### Consequences

* Good, because versions, Docker image tags and release notes are created consistently, without manual work.
* Good, because the team decides when a sprint release is made and whether it is a minor or a major release.
* Good, because pull request titles follow Conventional Commits, so the generated release notes are readable.
* Bad, because a release must be started by hand.

### Confirmation

The jobs `determine-version`, `push-docker-image` and `create-release` in `.github/workflows/build-test-deploy.yml`,
and the workflow `.github/workflows/check-for-conventional-commits.yml`.

## Pros and Cons of the Options

### Commit message annotations

* Good, because a pull request can start the release process.
* Bad, because it has to be clear when a pull request starts a release.

### Git tag

* Good, because the tag makes it obvious that a release is made.
* Bad, because the tag has to be created by hand.

### Release branch

* Good, because the branch name makes it clear that a release is created.
* Bad, because the release branch has to be created by hand.

### Release on every build

* Good, because every build creates a patch release.
* Bad, because the build cannot determine whether a release is minor or major.

### Manually started release workflow

* Good, because one input decides the version bump, and the workflow does everything else.
* Bad, because a release must be started by hand.

## More Information

The original decision of 2023-12-05 chose commit message annotations to determine the version bump. The release
workflow replaced these with a manual choice of version bump in
[#3451](https://github.com/infonl/dimpact-zaakafhandelcomponent/pull/3451) and added hotfix releases in
[#4109](https://github.com/infonl/dimpact-zaakafhandelcomponent/pull/4109).

Migrated from the Confluence decision log:
[Automated release notes and versioning](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/20709837).
