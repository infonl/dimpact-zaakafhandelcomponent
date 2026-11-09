---
status: proposed
date: 2026-11-03
decision-makers: Team Genève
tags: [release, ci, helm]
---

# Publish the Helm chart on gh-pages without GitHub releases

## Context and Problem Statement

The Helm chart release workflow uses `helm/chart-releaser-action`. It creates a Git tag and a GitHub release
`zaakafhandelcomponent-1.0.x` for every pull request that changes `charts/`, because it stores the chart packages as
release assets. There are 390 of them. GitHub sorts releases by creation date, so the chart releases push the ZAC
releases down the releases page. PodiumD pulls the chart from the Helm repository
`https://infonl.github.io/dimpact-zaakafhandelcomponent`. See [PZ-13388](https://dimpact.atlassian.net/browse/PZ-13388).

## Decision Drivers

* The releases page shows only ZAC releases.
* PodiumD keeps using the Helm repository URL without a change.
* The chart versions that PodiumD depends on stay available.
* Every chart version keeps a Git tag. For each sprint, the team picks the chart version for the sprint release and
  lists the images that changed since the previous chart version with `git diff` between two chart tags. They need
  that list to copy the images to the internal Docker registry of the SSC.

## Considered Options

* Keep `chart-releaser-action`
* Store the chart packages on the `gh-pages` branch
* Push the chart to an OCI registry (GHCR)

## Decision Outcome

Chosen option: "Store the chart packages on the `gh-pages` branch", because it is the only option that removes the
chart releases and needs no change in PodiumD.

* `helm-chart-release.yml` packages the chart into `helm-charts/` on `gh-pages` and updates `index.yaml`. It creates
  no release. It tags the commit with `zaakafhandelcomponent-<version>`, as `chart-releaser-action` did.
* `gh-pages` keeps the chart versions that a PodiumD branch or tag depends on, and every chart version from `1.0.340`
  onwards. The `zaakafhandelcomponent-*` releases are deleted after their packages are copied. Their tags are kept.

### Consequences

* Good, because the releases page only shows ZAC releases.
* Good, because the Helm repository URL does not change.
* Good, because the chart tags stay, so the images that changed between two chart versions are still easy to find.
* Bad, because every chart version adds about 0.4 MB to the `gh-pages` branch.
* Bad, because chart versions below `1.0.340` that no PodiumD branch or tag depends on can no longer be installed.

### Confirmation

The weekly `--check` of `scripts/github/cleanup-tags-and-releases.py` in
`.github/workflows/clean-up-tags-and-releases.yml` fails when a `zaakafhandelcomponent-*` release exists,
see [ADR-0014](0014-keep-only-release-tags-and-releases.md).

## Pros and Cons of the Options

### Keep `chart-releaser-action`

* Good, because nothing changes.
* Bad, because its `cr.sh` always runs `cr upload`, which creates a GitHub release for every chart version.

### Store the chart packages on the `gh-pages` branch

* Good, because it is a classic Helm repository on the URL that PodiumD already uses.
* Bad, because the workflow packages the chart and updates the index itself.

### Push the chart to an OCI registry (GHCR)

* Good, because OCI is the direction of Helm, and GHCR retention can work like that of the Docker images.
* Bad, because PodiumD must change its dependency to `oci://ghcr.io/...`.

## More Information

An OCI registry can be added later, next to `gh-pages`, in a new ADR.
