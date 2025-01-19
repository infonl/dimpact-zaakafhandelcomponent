#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

import requests
from bs4 import BeautifulSoup
import re

from version_data import VersionData

class GitHubTagVersionExtractor:
    def __init__(self, url, prefix = ''):
        self.url = url
        self.prefix = prefix

    def get_latest_version(self):
        # Send a GET request to the web page
        response = requests.get(self.url)

        # Parse the web page content
        soup = BeautifulSoup(response.content, 'html.parser')

        # Find all h2.a tags containing the version information
        commits = soup.find_all('div', class_='commit')

        # Extract the version numbers from a tags within the ul
        version_list = []
        for commit in commits:
            a_tag = commit.find('a')
            if a_tag:
                version_number = a_tag.text.strip()
                if re.match(rf'{re.escape(self.prefix)}[\d.]+', version_number):
                    href = a_tag['href']
                    if not href.startswith('http'):
                        href = requests.compat.urljoin(self.url, href)
                    version_list.append(VersionData(version_number.lstrip(self.prefix), href))

        # Return the latest version
        if version_list:
            return max(version_list)
        else:
            return None
