#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

class PodiumdVersionComparator:
    def __init__(self, version1, version2):
        self.version1 = version1
        self.version2 = version2

    def print_version_updated_table(self):
        print('\n# PodiumD version updates:')
        print(f'| Component | PodiumD {self.version1.app_version} | PodiumD {self.version2.app_version} |')
        print(f'|---|---|---|')

        for component_name in sorted(self.version1.component_versions):
            v1 = self.version1.component_versions[component_name].version
            v2 = ''
            if component_name in self.version2.component_versions:
                v2 = self.version2.component_versions[component_name].version
            if v1 == v2:
                print(f'| {component_name} | {v1} | {v2} |')
            else:
                print(f'| {component_name} | {v1} | **{v2}** |')
