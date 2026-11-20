#!/usr/bin/env python3

#  SPDX-FileCopyrightText: 2026 INFO.nl
#  SPDX-License-Identifier: EUPL-1.2+

import os
import re
import shutil
import subprocess

import requests
from bs4 import BeautifulSoup

from latest_version_printer import print_latest_version
from version_data import VersionData

SEMANTIC_VERSION = r'\d+\.\d+\.\d+'
BRP_PERSONEN_MOCK_TAG = rf'{SEMANTIC_VERSION}-\d{{12}}'
WILDFLY_RELEASE_TAG = rf'({SEMANTIC_VERSION}\.Final)'


def _github_token():
    token = os.environ.get('GITHUB_TOKEN') or os.environ.get('GH_TOKEN')
    if token:
        return token
    if shutil.which('gh'):
        result = subprocess.run(['gh', 'auth', 'token'], capture_output=True, text=True, check=False)
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
    # Without a token the GitHub API allows only 60 requests per hour.
    return None


def latest_github_release(repository, tag_pattern=rf'v?({SEMANTIC_VERSION})'):
    """
    The highest GitHub release of the repository, read from the GitHub API over all pages. Drafts, prereleases and
    tags that do not fully match the tag pattern (such as '-rc.1' or 'operator/v1.0.0') are skipped. The first group
    of the tag pattern is the version.
    """
    headers = {'Accept': 'application/vnd.github+json'}
    token = _github_token()
    if token:
        headers['Authorization'] = f'Bearer {token}'

    version_list = []
    url = f'https://api.github.com/repos/{repository}/releases?per_page=100'
    while url:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        for release in response.json():
            match = re.fullmatch(tag_pattern, release['tag_name'])
            if match and not release['draft'] and not release['prerelease']:
                version_list.append(VersionData(match.group(1), release['html_url']))
        url = response.links.get('next', {}).get('url')
    return max(version_list) if version_list else None


def latest_nginx_version(heading_text):
    """The version under the given heading ('Mainline version' or 'Stable version') of the nginx download page."""
    url = 'https://nginx.org/en/download.html'
    response = requests.get(url, timeout=30)
    response.raise_for_status()
    heading = BeautifulSoup(response.text, 'html.parser').find('h4', string=heading_text)
    if heading is None:
        return None
    for link in heading.find_next('table').find_all('a'):
        match = re.fullmatch(rf'nginx-({SEMANTIC_VERSION})', link.get_text(strip=True))
        if match:
            return VersionData(match.group(1), url)
    return None


def latest_solr_version():
    """The highest 'Solr x.y.z' heading of the Apache Solr downloads page, which has one per release line."""
    url = 'https://solr.apache.org/downloads.html'
    response = requests.get(url, timeout=30)
    response.raise_for_status()
    version_list = []
    for heading in BeautifulSoup(response.text, 'html.parser').find_all('h2'):
        match = re.match(rf'Solr ({SEMANTIC_VERSION})', heading.get_text(' ', strip=True))
        if match:
            version_list.append(VersionData(match.group(1), url))
    return max(version_list) if version_list else None


def latest_ghcr_tag(image, tag_pattern):
    """
    The highest tag of a public ghcr.io image that fully matches the tag pattern. Tags are compared on all their
    numbers, so a build timestamp after the version counts too.
    """
    token_response = requests.get(f'https://ghcr.io/token?scope=repository:{image}:pull', timeout=30)
    token_response.raise_for_status()
    response = requests.get(f'https://ghcr.io/v2/{image}/tags/list?n=1000', timeout=30,
                            headers={'Authorization': f'Bearer {token_response.json()["token"]}'})
    response.raise_for_status()
    tags = [tag for tag in response.json()['tags'] if re.fullmatch(tag_pattern, tag)]
    if not tags:
        return None
    latest_tag = max(tags, key=lambda tag: [int(number) for number in re.findall(r'\d+', tag)])
    owner, name = image.split('/')
    return VersionData(latest_tag, f'https://github.com/{owner}/{name}/pkgs/container/{name}')


def print_versions_of_supporting_components():
    """
    Print the latest version of the supporting components on the sprint hero page:

    - Fluent-bit            from https://github.com/fluent/fluent-bit/releases
    - Grafana               from https://github.com/grafana/grafana/releases
    - Grafana Loki          from https://github.com/grafana/loki/releases
    - Tempo                 from https://github.com/grafana/tempo/releases
    - nginx                 from https://nginx.org/en/download.html
    - Redis                 from https://github.com/redis/redis/releases
    - BRP Personen Mock     from https://github.com/brp-api/personen-mock/pkgs/container/personen-mock
    - Solr                  from https://solr.apache.org/downloads.html
    - WildFly               from https://github.com/wildfly/wildfly/releases
    - PABC                  from https://github.com/Platform-Autorisatie-Beheer-Component/PABC-API/releases
    """

    print_latest_version('Fluent-bit latest version', lambda: latest_github_release("fluent/fluent-bit"))
    print_latest_version('Grafana latest version', lambda: latest_github_release("grafana/grafana"))
    print_latest_version('Grafana Loki latest version', lambda: latest_github_release("grafana/loki"))
    print_latest_version('Tempo latest version', lambda: latest_github_release("grafana/tempo"))
    print_latest_version('nginx latest mainline version', lambda: latest_nginx_version("Mainline version"))
    print_latest_version('nginx latest stable version', lambda: latest_nginx_version("Stable version"))
    print_latest_version('Redis latest version', lambda: latest_github_release("redis/redis"))
    print_latest_version('BRP Personen Mock latest version',
                         lambda: latest_ghcr_tag("brp-api/personen-mock", BRP_PERSONEN_MOCK_TAG))
    print_latest_version('Solr latest version', latest_solr_version)
    print_latest_version('WildFly latest version',
                         lambda: latest_github_release("wildfly/wildfly", WILDFLY_RELEASE_TAG))
    print_latest_version('PABC latest version',
                         lambda: latest_github_release("Platform-Autorisatie-Beheer-Component/PABC-API"))


if __name__ == '__main__':
    print_versions_of_supporting_components()
