---
status: accepted
date: 2026-10-30
decision-makers: Team Genève
tags: [helm, testing, ci, tooling]
---

# Use helm-unittest for unit tests of the Helm chart

## Context and Problem Statement

The templates of the ZAC Helm chart contain logic: values switch between deployment variants, such as OPA as a
sidecar or as a separate deployment, and `required` and `fail` checks reject incomplete values. CI runs `ct lint`
and `ct install`, but `ct install` only shows that the chart installs with one set of values. No test checks what
the templates render for other values. How do we test the template logic?
See [PZ-13343](https://dimpact.atlassian.net/browse/PZ-13343).

## Decision Drivers

* Test the rendered manifests for several sets of values, including values that the chart must reject
* Run without a Kubernetes cluster, locally and in CI
* Tests that live next to the chart and are easy to read and write

## Considered Options

* helm-unittest
* `helm template` with assertions in a script
* `helm test` pods
* `ct install` only

## Decision Outcome

Chosen option: "helm-unittest", because it is the only option that tests the rendered manifests for several sets of
values without a cluster, in test suites written in YAML.

* The test suites live in `charts/zac/tests/`, one suite per topic, and `.helmignore` keeps them out of the packaged
  chart.
* The suites load `charts/zac/ci/zac-chart-test-values.yaml` and set only the values that a test is about.
* The tests assert on specific fields. We do not use snapshot tests, because they change with every chart change.

### Consequences

* Good, because a change that breaks the template logic fails CI before the chart is installed anywhere.
* Good, because the tests run in milliseconds and need no cluster.
* Bad, because the tests depend on a Helm plugin that has to be installed. Helm 4 cannot verify plugins installed
  from git, so the install uses `--verify=false`. On macOS the install script of the plugin needs GNU `sha256sum`.
* Bad, because the tests do not show that ZAC works with the rendered manifests. `ct install` still checks that the
  chart installs.

### Confirmation

The step "Run helm-unittest" in the `helm-lint` job of `.github/workflows/helm-chart-testing.yml` runs
`helm unittest charts/zac` in every pull request that changes the chart.

## Pros and Cons of the Options

### helm-unittest

* Good, because it renders the chart without a cluster and asserts on the rendered manifests.
* Good, because a test can expect a template to fail, which tests the `required` and `fail` checks and the values
  schema.
* Bad, because it is a Helm plugin, which CI and developers have to install.

### `helm template` with assertions in a script

* Good, because it needs no plugin.
* Bad, because every assertion is custom script code, which is harder to read and to maintain than a test suite.

### `helm test` pods

* Good, because it tests a deployed release in a real cluster.
* Bad, because it needs a cluster and a deployed release, and tests one set of values per release.

### `ct install` only

* Good, because CI already runs it.
* Bad, because it only shows that the chart installs with one set of values.

## More Information

* [helm-unittest](https://github.com/helm-unittest/helm-unittest)
* The chart README describes how to run the tests locally.
