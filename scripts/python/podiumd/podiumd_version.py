#
#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+
#

class PodiumdVersion:
    def __init__(self, tag_url, chart_version, app_version, component_versions = None):
        self.tag_url = tag_url
        self.chart_version = chart_version
        self.app_version = app_version
        self.component_versions = component_versions if component_versions else {}

    def __str__(self):
        components_str = '\n'.join([str(v) for v in self.component_versions.items()])
        return f'PodiumD: {self.app_version}\nChart: {self.chart_version}\nComponents:\n{components_str}'
