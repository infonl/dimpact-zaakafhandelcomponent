#!/usr/bin/env python3

#  SPDX-FileCopyrightText: 2026 INFO.nl
#  SPDX-License-Identifier: EUPL-1.2+

"""
Raises the Renovate allowedVersions caps of the PodiumD components to the versions of the newest PodiumD release, in
the ZAC repo and in the provisioning repo, so Renovate opens the update pull requests.

  renovate_caps.py plan <plan.json>
      Prints, per repo, every cap that changes (old -> new) and saves the plan. Changes nothing.
  renovate_caps.py apply <plan.json> <PZ-ticket>
      Per repo with changes: creates a branch from main, commits the new renovate.json and opens one pull request.
      Refuses when main or renovate.json changed since the plan.

Only the allowedVersions of the Docker images and Helm charts below count: images against the image tags in the
PodiumD values.yaml, charts against the chart versions in the PodiumD Chart.yaml. Open Archiefbeheer, the Bitnami
Redis chart (PodiumD uses redis-operator instead) and other rules are never changed. A cap only goes up, in the style of the other caps: `< X.<Y+1>.0` for PodiumD version
X.Y.Z, so the PodiumD version and its patches are allowed and the next minor is not. A cap that already allows the
PodiumD version stays as it is. Needs `gh`, logged in with push access to both repos.
"""

from __future__ import annotations

import base64
import json
import re
import subprocess
import sys
import urllib.request

REPOS = ['infonl/dimpact-zaakafhandelcomponent', 'infonl/dimpact-provisioning']
HELM_CHARTS = 'Dimpact-Samenwerking/helm-charts'

# Docker image -> (component name, path of its image block in the PodiumD values.yaml, required repository or None)
IMAGES = {
    'quay.io/keycloak/keycloak': ('Keycloak', ['keycloak', 'image'], None),
    'openformulieren/open-forms': ('Open Formulieren', ['openformulieren', 'image'], None),
    'maykinmedia/open-object': ('Open Object', ['objecten', 'image'], 'maykinmedia/open-object'),
    'maykinmedia/open-klant': ('Open Klant', ['openklant', 'image'], None),
    'openzaak/open-notificaties': ('Open Notificaties', ['opennotificaties', 'image'], None),
    'openzaak/open-zaak': ('Open Zaak', ['openzaak', 'image'], None),
    'redis': ('Redis', ['redis-operator', 'redis-ha', 'image'], None),
    'ghcr.io/platform-autorisatie-beheer-component/pabc-api': ('PABC', ['pabc', 'image'], None),
}

# (Renovate datasource, package) of a Helm chart -> (name, dependency name in the PodiumD Chart.yaml)
CHARTS = {
    ('helm', 'openforms'): ('Open Formulieren Helm chart', 'openforms'),
    ('helm', 'openobject'): ('Open Object Helm chart', 'openobject'),
    ('helm', 'openklant'): ('Open Klant Helm chart', 'openklant'),
    ('helm', 'opennotificaties'): ('Open Notificaties Helm chart', 'opennotificaties'),
    ('helm', 'openzaak'): ('Open Zaak Helm chart', 'openzaak'),
    ('docker', 'ghcr.io/platform-autorisatie-beheer-component/pabc'): ('PABC Helm chart (OCI)', 'pabc'),
}


class RenovateCapsError(Exception):
    pass


# ---------------------------------------------------------------- GitHub

def gh(*arguments, input_text=None):
    result = subprocess.run(['gh', *arguments], capture_output=True, text=True, input=input_text, check=False)
    if result.returncode != 0:
        raise RenovateCapsError(f'gh {" ".join(arguments[:3])} failed: {result.stderr.strip()[:500]}')
    return result.stdout


def gh_json(*arguments, input_text=None):
    return json.loads(gh(*arguments, input_text=input_text))


def newest_podiumd_release():
    tags = gh('api', '--paginate', f'repos/{HELM_CHARTS}/tags?per_page=100', '--jq', '.[].name').split()
    releases = [tag for tag in tags if re.fullmatch(r'podiumd-\d+\.\d+\.\d+', tag)]
    if not releases:
        raise RenovateCapsError('no podiumd-X.Y.Z tag found')
    return max(releases, key=lambda tag: [int(number) for number in re.findall(r'\d+', tag)])


def read_podiumd_file(tag, name):
    url = f'https://raw.githubusercontent.com/{HELM_CHARTS}/{tag}/charts/podiumd/{name}'
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read().decode()


def chart_dependencies(chart_yaml):
    """{dependency name: version} of the dependencies in a Chart.yaml (read by indentation)."""
    dependencies, current = {}, None
    in_dependencies = False
    for line in chart_yaml.splitlines():
        if re.match(r'^dependencies:\s*$', line):
            in_dependencies = True
            continue
        if in_dependencies and re.match(r'^\S', line):
            break
        if not in_dependencies:
            continue
        name = re.match(r'^\s*-\s*name:\s*["\']?([^"\'#\s]+)', line)
        if name:
            current = name.group(1)
            continue
        version = re.match(r'^\s+version:\s*["\']?(\d+\.\d+\.\d+)', line)
        if version and current and current not in dependencies:
            dependencies[current] = version.group(1)
    return dependencies


def podiumd_versions(release):
    """{(datasource, package): version} of the images and charts of a PodiumD release."""
    values = read_podiumd_file(release, 'values.yaml')
    dependencies = chart_dependencies(read_podiumd_file(release, 'Chart.yaml'))
    versions = {('docker', package): podiumd_version(values, path, repository)
                for package, (_, path, repository) in IMAGES.items()}
    versions.update({key: dependencies.get(dependency) for key, (_, dependency) in CHARTS.items()})
    return versions


def rule_name(key):
    return IMAGES[key[1]][0] if key[0] == 'docker' and key[1] in IMAGES else CHARTS[key][0]


# ---------------------------------------------------------------- PodiumD versions

def image_block(values, path):
    """The keys of the YAML block at path (read by indentation), as {key: value}; None when the path is missing."""
    lines = values.splitlines()
    start, end = 0, len(lines)
    for depth, key in enumerate(path):
        indent = ' ' * (2 * depth)
        index = next((number for number in range(start, end)
                      if re.fullmatch(rf'{re.escape(indent)}{re.escape(key)}:\s*(#.*)?', lines[number])), None)
        if index is None:
            return None
        start = index + 1
        end = next((number for number in range(start, end)
                    if lines[number].strip() and not lines[number].strip().startswith('#')
                    and len(lines[number]) - len(lines[number].lstrip()) <= 2 * depth), end)
    child_indent = ' ' * (2 * len(path))
    block = {}
    for line in lines[start:end]:
        match = re.fullmatch(rf'{child_indent}([A-Za-z]+):\s*(.*?)\s*(#.*)?', line)
        if match:
            block[match.group(1)] = match.group(2)
    return block


def podiumd_version(values, path, required_repository):
    block = image_block(values, path)
    if not block or 'tag' not in block:
        return None
    if required_repository and required_repository not in block.get('repository', ''):
        return None
    tag = re.sub(r'^&\S+\s+', '', block['tag']).strip('"\'')
    match = re.match(r'v?(\d+\.\d+\.\d+)', tag)
    return match.group(1) if match else None


# ---------------------------------------------------------------- caps

def _numbers(version):
    return [int(number) for number in re.findall(r'\d+', version)]


def allows(constraint, version):
    """Whether a simple allowedVersions constraint ('< X', '<= X', '< X.Y') allows version; None if unknown."""
    match = re.fullmatch(r'\s*(<=?)\s*v?([\d.]+)\s*', constraint)
    if not match:
        return None
    limit, ours = _numbers(match.group(2)), _numbers(version)
    width = max(len(limit), len(ours))
    limit += [0] * (width - len(limit))
    ours += [0] * (width - len(ours))
    return ours <= limit if match.group(1) == '<=' else ours < limit


def plan_repo(text, versions):
    """Returns (new_text, changes, unchanged) for one renovate.json text."""
    rules = json.loads(text)['packageRules']
    capped = [rule for rule in rules if 'allowedVersions' in rule]
    positions = [match for match in re.finditer(r'"allowedVersions"\s*:\s*"((?:[^"\\]|\\.)*)"', text)]
    if len(positions) != len(capped):
        raise RenovateCapsError('cannot line up the allowedVersions in the text with the package rules')
    edits, changes, unchanged = [], [], []
    for rule, position in zip(capped, positions):
        datasources = rule.get('matchDatasources') or []
        if len(datasources) != 1:
            continue
        keys = [(datasources[0], package) for package in rule.get('matchPackageNames', [])
                if (datasources[0], package) in versions]
        if not keys:
            continue
        name = rule_name(keys[0])
        version = versions[keys[0]]
        old = rule['allowedVersions']
        if version is None:
            unchanged.append((name, old, 'not in this PodiumD release'))
            continue
        allowed = allows(old, version)
        if allowed is None:
            unchanged.append((name, old, f'unknown constraint, PodiumD has {version}'))
        elif allowed:
            unchanged.append((name, old, f'already allows PodiumD {version}'))
        else:
            major, minor = _numbers(version)[:2]
            new = f'< {major}.{minor + 1}.0'
            edits.append((position.start(1), position.end(1), new))
            changes.append({'component': name, 'packages': rule['matchPackageNames'], 'datasources': datasources,
                            'old': old, 'new': new})
    new_text = text
    for start, end, replacement in sorted(edits, reverse=True):
        new_text = new_text[:start] + replacement + new_text[end:]
    check_only_caps_changed(text, new_text, changes)
    return new_text, changes, unchanged


def check_only_caps_changed(old_text, new_text, changes):
    old_json, new_json = json.loads(old_text), json.loads(new_text)
    expected = {json.dumps([change['packages'], change['datasources']]): change['new'] for change in changes}
    for old_rule, new_rule in zip(old_json['packageRules'], new_json['packageRules']):
        key = json.dumps([old_rule.get('matchPackageNames'), old_rule.get('matchDatasources')])
        if key in expected:
            old_rule = dict(old_rule, allowedVersions=expected[key])
        if old_rule != new_rule:
            raise RenovateCapsError(f'self-check failed: unexpected change in rule {key}')
    old_json.pop('packageRules'), new_json.pop('packageRules')
    if old_json != new_json:
        raise RenovateCapsError('self-check failed: something outside packageRules changed')


# ---------------------------------------------------------------- commands

def plan(plan_file):
    release = newest_podiumd_release()
    versions = podiumd_versions(release)
    result = {'podiumd': release, 'repos': []}
    print(f'Newest PodiumD release: {release}')
    for repo in REPOS:
        main_sha = gh('api', f'repos/{repo}/commits/main', '--jq', '.sha').strip()
        file = gh_json('api', f'repos/{repo}/contents/renovate.json?ref={main_sha}')
        text = base64.b64decode(file['content']).decode()
        new_text, changes, unchanged = plan_repo(text, versions)
        print(f'\n## {repo}')
        for change in changes:
            print(f'- {change["component"]}: allowedVersions "{change["old"]}" -> "{change["new"]}"')
        if not changes:
            print('- no caps to raise')
        for name, old, reason in unchanged:
            print(f'  unchanged: {name} "{old}" ({reason})')
        result['repos'].append({'repo': repo, 'main_sha': main_sha, 'file_sha': file['sha'],
                                'new_text': new_text, 'changes': changes})
    with open(plan_file, 'w', encoding='utf-8') as handle:
        json.dump(result, handle)
    print(f'\nPlan saved to {plan_file}. Nothing has been changed.')


def existing_pull_request(repo, branch):
    refs = gh_json('api', f'repos/{repo}/git/matching-refs/heads/{branch}')
    if not any(ref['ref'] == f'refs/heads/{branch}' for ref in refs):
        return None
    pull_requests = gh_json('pr', 'list', '-R', repo, '--head', branch, '--state', 'open', '--json', 'url')
    if pull_requests:
        return pull_requests[0]['url']
    raise RenovateCapsError(f'{repo}: branch {branch} already exists without an open pull request; delete it '
                            f'(or open its pull request), then run apply again')


def apply(plan_file, ticket):
    if not re.fullmatch(r'PZ-\d+', ticket):
        raise RenovateCapsError(f'not a PZ ticket: {ticket}')
    with open(plan_file, encoding='utf-8') as handle:
        planned = json.load(handle)
    release = planned['podiumd']
    version = release.removeprefix('podiumd-')
    branch = f'feature/{ticket}-renovate-podiumd-{version}'
    title = f'chore(renovate): allow the component versions of PodiumD {version}'
    done = {}
    for entry in planned['repos']:
        if not entry['changes']:
            continue
        repo = entry['repo']
        url = existing_pull_request(repo, branch)
        if url:
            done[repo] = url
            continue
        if gh('api', f'repos/{repo}/commits/main', '--jq', '.sha').strip() != entry['main_sha'] or \
                gh('api', f'repos/{repo}/contents/renovate.json', '--jq', '.sha').strip() != entry['file_sha']:
            raise RenovateCapsError(f'{repo}: main or renovate.json changed since the plan; make a new plan')
    for entry in planned['repos']:
        if not entry['changes']:
            print(f'{entry["repo"]}: no caps to raise, no pull request')
            continue
        repo = entry['repo']
        if repo in done:
            print(f'{repo}: pull request already exists, skipped: {done[repo]}')
            continue
        lines = [f'- {change["component"]}: `{change["old"]}` -> `{change["new"]}`' for change in entry['changes']]
        gh('api', '-X', 'POST', f'repos/{repo}/git/refs', '-f', f'ref=refs/heads/{branch}', '-f',
           f'sha={entry["main_sha"]}')
        commit_message = (f'{title}\n\nRaise the Renovate allowedVersions caps of the images and Helm charts to the versions of {release}:\n'
                          + '\n'.join(lines) + '\n')
        gh('api', '-X', 'PUT', f'repos/{repo}/contents/renovate.json', '--input', '-', input_text=json.dumps({
            'message': commit_message,
            'content': base64.b64encode(entry['new_text'].encode()).decode(),
            'sha': entry['file_sha'],
            'branch': branch,
        }))
        body = (f'Raises the Renovate `allowedVersions` caps of the images and Helm charts to the versions of the newest PodiumD release, '
                f'[{release}](https://github.com/{HELM_CHARTS}/blob/{release}/charts/podiumd/values.yaml), so Renovate '
                f'opens the update pull requests:\n\n' + '\n'.join(lines)
                + '\n\nThe caps of the other repo are raised in a pull request of their own, so both stay aligned.'
                + f'\n\nSolves {ticket}\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n')
        url = gh('pr', 'create', '-R', repo, '--base', 'main', '--head', branch, '--title', title, '--body', body)
        print(f'{repo}: {url.strip()}')


def main(arguments):
    commands = {'plan': (plan, 1), 'apply': (apply, 2)}
    if not arguments or arguments[0] not in commands or len(arguments) - 1 != commands[arguments[0]][1]:
        print(__doc__, file=sys.stderr)
        return 2
    try:
        commands[arguments[0]][0](*arguments[1:])
    except RenovateCapsError as error:
        print(f'ERROR: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
