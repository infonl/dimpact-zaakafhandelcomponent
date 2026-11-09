# Security issues in component repositories

Repos (fixed order): `infonl/dimpact-zaakafhandelcomponent`, `infonl/webdav-servlet`.

Columns: Component Repository | Last Checked | Security Issues | Open | New | Closed | Notes.

## Period

New and Closed count from the "Last Checked" date of each repo in the pasted table (the previous check). Without a pasted table, ask for the sprint start date; if the user leaves it open, use the last 14 days and say so.

## Source

GitHub code-scanning alerts, read-only. All tools in the repo count (CodeQL, NPM Audit, OWASP Dependency Check, Snyk, Trivy). Reading them needs write access to the repo.

```bash
gh api --paginate "repos/infonl/<repo>/code-scanning/alerts?per_page=100" \
  --jq '.[] | select(.rule.security_severity_level != null)
        | [.state, .rule.security_severity_level, .rule.id, .created_at[:10], ((.fixed_at // .dismissed_at // "")[:10])] | @tsv'
```

- Skip alerts without `rule.security_severity_level`: these are code-quality findings, not security issues.
- Count unique `rule.id` per severity, not alerts. One CVE found in several jars or lock files counts once.
- Open: `state == open` now.
- New: `created_at >= <since>`, whatever the state is now.
- Closed: `state` is `fixed` or `dismissed` and `fixed_at`/`dismissed_at >= <since>`. Take the union of both states, so a rule that was fixed in one place and dismissed in another counts once.
- Last Checked: the date of the newest analysis, `gh api "repos/infonl/<repo>/code-scanning/analyses?per_page=1" --jq '.[0].created_at'`.

## Output

One rendered markdown table, NOT in a code block. Per repo: a row with only the repo name, then the rows Critical, High, Medium, Low (Last Checked on the Critical row), then a `**Total**` row for that repo. End with one `**Total**` row for all repos. Notes stays empty, unless a remark in the pasted table still holds.

Below the table, max 4 short lines:

- How many of the Closed rules were dismissed instead of fixed, and which rules are both closed and open again.
- Open Dependabot alerts per severity, if any (`gh api "repos/infonl/<repo>/dependabot/alerts?state=open&per_page=100"`). They are not in the table; a 403 "disabled" means Dependabot is off for that repo.
- The cell colours on the page are manual; ask the user to let Rovo update them.
