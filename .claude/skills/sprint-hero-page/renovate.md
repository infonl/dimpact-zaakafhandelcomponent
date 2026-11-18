# Raise the Renovate caps

Offer this at the end of the tasks PodiumD componenten and Supporting componenten, after the table (and its update in
Confluence) is done. Not after Externe API's: the API specs are not updated by Renovate.

The ZAC repo and `infonl/dimpact-provisioning` cap the PodiumD components in `renovate.json` with
`allowedVersions` ("Pinned by the next Dimpact PodiumD release"), both their Docker image and the Helm chart that
installs it. When the newest PodiumD release has a version above a cap, Renovate does not open the update pull
request. The chart is raised together with the image, so a new image does not run on a chart that is too old for it. This raises those caps to the PodiumD version, in one pull request
per repo, so both repos stay aligned.

`renovate_caps.py` (next to this file) does the work. Run it with
`python3 .claude/skills/sprint-hero-page/renovate_caps.py` and the sandbox disabled (gh keychain).

## Scope

- The Docker images of Keycloak, Open Formulieren, Open Object, Open Klant, Open Notificaties, Open Zaak, Redis and
  PABC, and the Helm charts `openforms`, `openobject`, `openklant`, `opennotificaties`, `openzaak` and the PABC OCI
  chart, only when they are in that repo's `renovate.json`. Never Open Archiefbeheer, the Bitnami `redis` chart
  (PodiumD installs Redis with redis-operator, so it has no version for it) or other rules.
- The target is the version in the newest PodiumD release (highest `podiumd-X.Y.Z` tag), not Latest: for an image the
  tag in its `values.yaml`, for a chart the dependency version in its `Chart.yaml`. The new cap keeps
  the style of the other caps: `< X.<Y+1>.0` for PodiumD version X.Y.Z (PodiumD 4.3.0 gives `< 4.4.0`), so that
  version and its patches are allowed and the next minor is not. A cap only goes up; one that already allows the
  PodiumD version stays.

## Steps

0. Check that the `gh` login may push to both repos: `gh api repos/<repo> --jq .permissions.push` must print `true` for `infonl/dimpact-zaakafhandelcomponent` and `infonl/dimpact-provisioning`. If not, say so and stop after the plan.
1. `renovate_caps.py plan <plan.json>` in the scratchpad. It prints, per repo, every cap that changes and the ones
   that stay. If no cap changes, say so in one line and stop: there is nothing to offer.
2. Offer the pull requests: show the change list per repo and ask whether to create them. Ask for the Jira ticket of
   this sprint's hero task for raising the Renovate caps (`PZ-XXXX`); show its summary with
   `acli jira workitem view <ticket>` so the user can check it is the right one.
3. Only after an explicit yes, run `renovate_caps.py apply <plan.json> <ticket>`. Each yes covers one plan only.
   - Per repo with changes it creates the branch `feature/<ticket>-renovate-podiumd-<version>` from `main`, commits
     only `renovate.json` and opens one pull request titled
     `chore(renovate): allow the component versions of PodiumD <version>`, ending with `Solves <ticket>`.
   - It works through the GitHub API; the user's local checkout and branch are not touched.
   - It refuses when `main` or `renovate.json` changed since the plan: make a new plan and ask again.
   - It skips a repo whose branch already has an open pull request, so it can be run again after a partial failure.
     When the branch exists without an open pull request, it stops before any write; ask the user to delete that branch.
4. Give the user the pull request links. Merging stays with the user.
