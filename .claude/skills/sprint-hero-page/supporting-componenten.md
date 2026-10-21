# Supporting componenten

Rows (fixed order): Fluent-bit, Grafana, Grafana Loki, Tempo, nginx, Redis, BRP Personen Mock, Solr, WildFly, PABC.

Columns: Latest | Our Version on TEST.

## 1. Latest

GitHub releases via `gh api --paginate "repos/<repo>/releases?per_page=100"`: inspect every page and take the highest non-prerelease, non-draft version. Release lists are not sorted by version, so compare numbers, not order. The prerelease flag is not always set, so also skip versions with a suffix such as `-rc.1`, `Alpha` or `Beta`.

- Fluent-bit: `fluent/fluent-bit` (patches of older lines can be released after the newest one; still take the highest number)
- Grafana: `grafana/grafana`
- Grafana Loki: `grafana/loki` (ignore `operator/…` tags)
- Tempo: `grafana/tempo`
- Redis: `redis/redis`
- WildFly: `wildfly/wildfly` (`X.Y.Z.Final`, skip Beta)
- PABC: `Platform-Autorisatie-Beheer-Component/PABC-API`. Also mention a newer prerelease, if there is one.

Other sources:

- nginx: https://nginx.org/en/download.html, shown as `<mainline> (mainline) / <stable> (stable)`.
- BRP Personen Mock: ghcr.io `brp-api/personen-mock`. Get an anonymous token from `https://ghcr.io/token?scope=repository:brp-api/personen-mock:pull`, then read `https://ghcr.io/v2/brp-api/personen-mock/tags/list?n=1000`. Take the highest `X.Y.Z-<12-digit timestamp>` tag, not `-latest`.
- Solr: https://solr.apache.org/downloads.html. To check the remark about the Solr Operator, read its latest release via `gh api repos/apache/solr-operator/releases/latest` and the supported Solr versions in the "Version Compatibility Matrixes" section of `gh api "repos/apache/solr-operator/contents/docs/modules/upgrade-notes/pages/upgrade-notes.adoc?ref=<latest release tag>" --jq .content | base64 -d`. Read it at the release tag, not the default branch, which already lists unreleased versions. Count Solr 10 as supported only when a column explicitly names 10; an open-ended column such as "9.4+" does not count.

## 2. Our Version on TEST

PRIVATE repo `infonl/dimpact-provisioning`, branch `main`, via `gh api repos/infonl/dimpact-provisioning/contents/<path> --jq .content | base64 -d`, unless stated otherwise. Strip `@sha256…` and image variant suffixes such as `-alpine` and `-slim`.

- Fluent-bit: image in `containers/fluent-bit/fluent-bit.yml`.
- nginx: image in `containers/https-proxy/nginx.yml`.
- Redis: `redis_image_tag` default in `.github/workflows/azure-provision-redis.yml`. To check the remark "in sync with PodiumD", compare it with `redis-operator.redis-ha.image.tag` (image `quay.io/opstree/redis`; strip the `v` prefix) in `https://raw.githubusercontent.com/Dimpact-Samenwerking/helm-charts/<tag>/charts/podiumd/values.yaml` of the newest PodiumD release: the highest tag matching `^podiumd-[0-9]+\.[0-9]+\.[0-9]+$` by `sort -V`, from `gh api --paginate "repos/Dimpact-Samenwerking/helm-charts/tags?per_page=100" --jq '.[].name'`. In sync means an exact X.Y.Z match; otherwise report both versions and the PodiumD tag.
- BRP Personen Mock: image in `containers/brp/brp.yml`.
- PABC: `pabc_api_docker_image_override` default in `.github/workflows/azure-provision-pabc.yml`.
- Grafana, Grafana Loki, Tempo: these come from the Dimpact `monitoring-logging` chart.
  1. Read `MONITORING_LOGGING_CHART_VERSION` in `.github/workflows/azure-provision-trace-and-metrics.yml`. This is the only source of the chart version; ignore version numbers in comments elsewhere. Name this chart version in the lines below the table, so the remark on the page can be updated.
  2. Download the published chart `https://github.com/Dimpact-Samenwerking/helm-charts/releases/download/monitoring-logging-<version>/monitoring-logging-<version>.tgz` and unpack it.
  3. Use these exact paths in `values.yaml`; the sections also hold sidecar images (e.g. `alpine…`), so never take just any `tag:`. Grafana = `grafana.image.tag`. Loki = `loki.loki.image.tag`. Tempo = `tempo.tempo.image.tag`. When a path has no tag, use `appVersion` in `charts/<grafana|loki|tempo>/Chart.yaml`; when it has one, cross-check it against that `appVersion` and mention a difference.
  4. Check `containers/metrics/monitoring-logging-values.yaml` in the provisioning repo for image overrides and for `<grafana|loki|tempo>.enabled`. The chart's own default can be `enabled: false` (Tempo); a component that is not enabled after the overrides is not deployed: show `-` and say so.
- Solr: ZAC repo, `git show origin/main:charts/zac/values.yaml`, the `library/solr` image tag.
- WildFly: ZAC repo, `<wildfly.version>` in `git show origin/main:pom.xml`.
- These are the pins for the next deploy; a manual run with a tag can override them.
