#
#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+
#

class PodiumdVersion:
    """
    PodiumD version information data object.
    """
    def __init__(self, tag_url, chart_version, app_version, chart_dependency_versions = None, component_versions = None):
        self.tag_url = tag_url
        self.chart_version = chart_version
        self.app_version = app_version
        self.chart_dependency_versions = chart_dependency_versions if chart_dependency_versions else {}
        self.component_versions = component_versions if component_versions else {}

    def __str__(self):
        chart_dependencies_str = '\n'.join([f'{k}: {v}' for k, v in self.chart_dependency_versions.items()])
        components_str = '\n'.join([f'{k}: {v}' for k, v in self.component_versions.items()])
        return f'PodiumD: {self.app_version}\nChart: {self.chart_version}\nChart Dependencies:\n{chart_dependencies_str}\nComponents:\n{components_str}'
