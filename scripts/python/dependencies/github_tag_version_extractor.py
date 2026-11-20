#  SPDX-FileCopyrightText: 2024 INFO.nl
#  SPDX-License-Identifier: EUPL-1.2+

import re
import subprocess

from version_data import VersionData

class GitHubTagVersionExtractor:
    """
    Version extractor for the tags of a GitHub repository.

    Reads all tags with `git ls-remote`, so it is not limited to the first page of the tags page and needs no GitHub
    API token. Only tags that are exactly '<prefix>X.Y.Z' count, so pre-releases such as '4.1.0-alpha.2' or
    '5.13.0-dev' are skipped. With hotfix_suffix, ZAC's hotfix tags '<prefix>X.Y.Z-N' (ADR-0014) count too, and
    'X.Y.Z-N' is higher than 'X.Y.Z'.
    """

    def __init__(self, url, prefix = '', hotfix_suffix = False):
        self.url = url
        self.prefix = prefix
        self.hotfix_suffix = hotfix_suffix

    def get_latest_version(self):
        repository_url = re.sub(r'/tags/?$', '', self.url)
        result = subprocess.run(['git', 'ls-remote', '--tags', '--refs', f'{repository_url}.git'],
                                capture_output=True, text=True, timeout=120, check=True)

        suffix = r'(?:-\d+)?' if self.hotfix_suffix else ''
        versions = []
        for line in result.stdout.splitlines():
            tag = line.split('refs/tags/', 1)[-1]
            match = re.fullmatch(rf'{re.escape(self.prefix)}(\d+\.\d+\.\d+{suffix})', tag)
            if match:
                versions.append((match.group(1), tag))

        if not versions:
            return None
        # VersionData cannot order 'X.Y.Z-N', so order by all numbers; a missing hotfix number counts as 0.
        version, tag = max(versions, key=lambda entry: [int(number) for number in re.findall(r'\d+', entry[0])] + [0])
        return VersionData(version, f'{repository_url}/releases/tag/{tag}')
