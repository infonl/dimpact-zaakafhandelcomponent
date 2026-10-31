#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

import requests
from bs4 import BeautifulSoup
import re

from version_data import VersionData


class KvkVersionExtractor:
    """
    Version extractor specifically for a KvK release notes pages.
    """

    def __init__(self, url):
        self.url = url

    def get_latest_version(self):
        # Send a GET request to the web page
        response = requests.get(self.url)

        # Parse the web page content
        soup = BeautifulSoup(response.content, 'html.parser')

        # Find all version numbers matching the pattern 'Versi(on|e) x.y.z'
        releases = soup.find_all('h2', id=lambda x: x and x.startswith('Release'))
        versions = [header.get_text(strip=True).replace('Release', '').strip() for header in releases]

        # Extract the version numbers from a tags within the ul
        version_list = []

        for version_number in versions:
            if re.match(r'[\d.]+', version_number):
                version_data = VersionData(version_number, self.url)
                version_list.append(version_data)


        # Return the latest version
        if version_list:
            return max(version_list)
        else:
            return None
