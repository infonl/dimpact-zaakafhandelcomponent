---
status: proposed
date: 2026-11-03
decision-makers: Team Genève
tags: [release, ci]
---

# Keep only release tags and releases

## Context and Problem Statement

The repository has 4,012 Git tags and 3,861 GitHub releases. Before ZAC 4.7 every build of the main branch created a
`vX.Y.Z` tag and release. [ADR-0002](0002-automate-release-notes-and-versioning.md) replaced that with a sprint
release, a hotfix release and one development pre-release, but the old tags and releases remain. Which tags and
releases do we keep, and how do we prevent that they grow again? See
[PZ-13388](https://dimpact.atlassian.net/browse/PZ-13388).

## Decision Drivers

* The releases page shows the releases that matter.
* The version calculation and the release notes of ADR-0002 keep working.
* The source of every ZAC version that PodiumD has deployed stays traceable.

## Considered Options

* Keep all tags and releases
* Keep the release tags only, and check the policy every week

## Decision Outcome

Chosen option: "Keep the release tags only, and check the policy every week", because it is the only option that
makes the releases page usable.

The repository keeps:

* every `vX.Y.0` tag and release (the sprint releases),
* every tag and release from `v4.7.0` onwards (the release process of ADR-0002),
* every ZAC version that a PodiumD release has deployed (`zac.image.tag` in `Dimpact-Samenwerking/helm-charts`),
* the `vX.Y.Z-dev` pre-release and the hotfix tags `vX.Y.Z-N`.

All other tags and releases are deleted. `scripts/github/cleanup-tags-and-releases.py` contains the policy.

### Consequences

* Good, because the number of tags and releases drops from about 4,000 to about 75.
* Good, because the version calculation uses the highest `vX.Y.Z` tag, which is kept.
* Bad, because the patch builds of the old process, apart from the deployed ones, can no longer be checked out by tag.
  Their commits stay on the main branch.

### Confirmation

The workflow `.github/workflows/clean-up-tags-and-releases.yml` runs
`scripts/github/cleanup-tags-and-releases.py --check` every week and fails when a tag or release does not match the
policy.

## Pros and Cons of the Options

### Keep all tags and releases

* Good, because nothing is lost.
* Bad, because the releases page stays cluttered with about 3,800 releases that nobody uses.

### Keep the release tags only, and check the policy every week

* Good, because the releases page shows the sprint and hotfix releases.
* Bad, because the deletion must be started by hand once, and it takes several hours due to the GitHub rate limit.

## More Information

[ADR-0011](0011-publish-the-helm-chart-on-gh-pages-without-github-releases.md) removes the chart releases.
`clean-up-old-zac-docker-images.yml` keeps every `X.Y.Z` Docker image; aligning it with this policy is a separate
change.
