#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

import requests

from version_data import VersionData


class DockerHubVersionExtractor:
    """
    Version extractor specifically for Docker Hub.
    """

    def __init__(self, slug):
        self.slug = slug

    def get_latest_version(self):
        # Send a GET request to the web page
        url = f"https://hub.docker.com/v2/repositories/{self.slug}/tags?page_size=25&page=1&ordering=last_updated&name="
        response = requests.get(url)
        response.raise_for_status()  # Check if the request was successful

        # Parse the JSON response
        data = response.json()

        # Extract the tags and create VersionData entities
        version_list = []
        for result in data['results']:
            tag_name = result['name']
            tag_digest = result['digest']
            tag_url = f"https://hub.docker.com/layers/{self.slug}/{tag_name}/images/{tag_digest.replace(':', '-')}"
            version_data = VersionData(tag_name, tag_url)
            version_list.append(version_data)

        # Return the latest version
        if version_list:
            return max(version_list)
        else:
            return None
