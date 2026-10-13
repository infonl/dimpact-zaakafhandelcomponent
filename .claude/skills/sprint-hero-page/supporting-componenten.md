# Supporting componenten

Rows (fixed order): Fluent-bit, Grafana, Grafana Loki, Tempo, nginx, Redis, BRP Personen Mock, Solr, WildFly, PABC.

Columns: Latest | Our Version on TEST.

## 1. Latest

GitHub releases via `gh api repos/<repo>/releases`: the highest non-prerelease, non-draft version. Release lists are not sorted by version, so compare numbers, not order.

- Fluent-bit: `fluent/fluent-bit` (ignore older maintenance lines such as 5.0.x)
- Grafana: `grafana/grafana`
- Grafana Loki: `grafana/loki` (ignore `operator/…` tags)
- Tempo: `grafana/tempo`
- Redis: `redis/redis`
- WildFly: `wildfly/wildfly` (`X.Y.Z.Final`, skip Beta)
- PABC: `Platform-Autorisatie-Beheer-Component/PABC-API`. Also mention a newer prerelease, if there is one.

Other sources:

- nginx: https://nginx.org/en/download.html, shown as `<mainline> (mainline) / <stable> (stable)`.
- BRP Personen Mock: ghcr.io `brp-api/personen-mock`. Get an anonymous token from `https://ghcr.io/token?scope=repository:brp-api/personen-mock:pull`, then read `https://ghcr.io/v2/brp-api/personen-mock/tags/list?n=1000`. Take the highest `X.Y.Z-<12-digit timestamp>` tag, not `-latest`.
- Solr: https://solr.apache.org/downloads.html

## 2. Our Version on TEST

PRIVATE repo `infonl/dimpact-provisioning`, branch `main`, via `gh api repos/infonl/dimpact-provisioning/contents/<path> --jq .content | base64 -d`, unless stated otherwise. Strip `@sha256…`.

- Fluent-bit: image in `containers/fluent-bit/fluent-bit.yml`.
- nginx: image in `containers/https-proxy/nginx.yml`.
- Redis: `redis_image_tag` default in `.github/workflows/azure-provision-redis.yml`.
- BRP Personen Mock: image in `containers/brp/brp.yml`.
- PABC: `pabc_api_docker_image_override` default in `.github/workflows/azure-provision-pabc.yml`.
- Grafana, Grafana Loki, Tempo: these come from the Dimpact `monitoring-logging` chart.
  1. Read `MONITORING_LOGGING_CHART_VERSION` in `.github/workflows/azure-provision-trace-and-metrics.yml`.
  2. Download the published chart `https://github.com/Dimpact-Samenwerking/helm-charts/releases/download/monitoring-logging-<version>/monitoring-logging-<version>.tgz` and unpack it.
  3. Grafana = `grafana.image.tag` in `values.yaml`. Loki = the loki `image.tag` in `values.yaml`. Tempo = the tempo `image.tag` in `values.yaml`, or, when it has none, `appVersion` in `charts/tempo/Chart.yaml`. Use the same fallback for Grafana and Loki.
  4. Check `containers/metrics/monitoring-logging-values.yaml` in the provisioning repo for image overrides.
- Solr: ZAC repo, `git show origin/main:charts/zac/values.yaml`, the `library/solr` image tag.
- WildFly: ZAC repo, `<wildfly.version>` in `git show origin/main:pom.xml`.
- These are the pins for the next deploy; a manual run with a tag can override them.
