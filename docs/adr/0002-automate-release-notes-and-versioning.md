---
status: accepted
date: 2023-12-05
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

## Considered Options

* Commit message annotations that indicate the version bump
* A Git tag that starts the release
* A release branch that starts the release
* A release on every build of the main branch

## Decision Outcome

Chosen option: "Commit message annotations that indicate the version bump", combined with automated Git versioning:
the version is derived from the commit messages, the ZAC Docker image is tagged with this version, and the release
notes for this version are published on GitHub automatically.

### Consequences

* Good, because versions and release notes are created consistently, without manual work.
* Bad, because the setup is non-trivial and relies on third-party open source tooling.

### Confirmation

The release workflow in `.github/workflows/build-test-deploy.yml`.

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
* Bad, because the build cannot determine whether a release is minor or major; it works well only in combination
  with commit message annotations for minor and major releases.

## More Information

Migrated from the Confluence decision log:
[Automated release notes and versioning](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/20709837).
