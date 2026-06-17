# Security Policy

The ZAC development team takes security of our application seriously.
We very much appreciate your help in identifying and addressing any security vulnerabilities.
While we have automated processes in place to help preventing security vulnerabilities in the application as much as possible,
we realize that this will never give a 100% guarantee.

## Supported versions

Security fixes are always applied to the **latest released version**.
For versions actively supported by [PodiumD](https://www.podiumd.nl/), we will provide backports for **Critical** severity vulnerabilities where this is feasible.

## Reporting a vulnerability

If you believe you have found a security vulnerability in our code base, please report it to us through coordinated disclosure.

Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.

Instead, please use one of the following channels:

- **Preferred**: [GitHub Security Advisories](https://github.com/infonl/dimpact-zaakafhandelcomponent/security/advisories/new) — opens a private report directly with the team.
- **Email**: [the maintainers](mailto:dimpact-team-geneve-d-aaaakaayu7miexe5zhcylrwe7a@infonl.slack.com).

Please include as much of the information listed below as you can to help us better understand and resolve the issue:

- The type of issue (e.g., buffer overflow, SQL injection, or cross-site scripting)
- Full paths of source file(s) related to the manifestation of the issue
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit the issue

This information will help us triage your report more quickly.

## Our response process

We handle all security reports through **coordinated disclosure**:

| Step | Description |
|---|---|
| Acknowledge receipt of your report | We confirm we have received your report |
| Confirm whether the issue is valid and assess severity | We investigate and assign a CVSS v3 severity rating |
| Provide a fix timeline and keep you updated on progress | We communicate our plan and any updates during the embargo period |
| Release a fix and publish a Security Advisory | We release the patch and disclose publicly, including any backports |
| Credit you in the advisory (if desired) | We credit you at the time of publication unless you prefer to remain anonymous |

We will communicate expected timelines once we have assessed the severity of the report.
If we need more time than originally indicated we will contact you to agree on an extended embargo period.

## CVE assignment

For confirmed vulnerabilities we will request a CVE identifier via GitHub's CVE Numbering Authority (CNA) programme. The CVE will be published together with the GitHub Security Advisory at the time of public disclosure.

## Our commitment to reporters

- We will not take legal action against researchers who follow responsible disclosure practices.
- We will keep you informed throughout the process.
- We will credit you in the published advisory unless you prefer to remain anonymous.
