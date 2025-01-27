#
#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+
#
import urllib.request
import yaml

from podiumd_version import PodiumdVersion
from component_version import ComponentVersion


class PodiumdVersionBuilder:
    """
    Builder for PodiumD Version information. It will pull the information from the Dimpact-Samenwerking GitHub pages,
    specifically from the Chart and Values files.
    """

    def __init__(self, version=None):
        self.podiumd_root_path = "https://raw.githubusercontent.com/Dimpact-Samenwerking/helm-charts/refs/heads/main"
        if version:
            self.podiumd_root_path = f"https://raw.githubusercontent.com/Dimpact-Samenwerking/helm-charts/refs/tags/podiumd-{version}"
        self.chart_file = f"{self.podiumd_root_path}/charts/podiumd/Chart.yaml"
        self.values_file = f"{self.podiumd_root_path}/charts/podiumd/values.yaml"
        self.products_table = {
            'openzaak': 'Open Zaak',
            'opennotificaties': 'Open Notificaties',
            'objecten': 'Objecten',
            'objecttypen': 'Objecttypen',
            'openklant': 'Open Klant',
            'openformulieren': 'Open Formulieren',
            'openinwoner': 'Open Inwoner',
            'kiss.frontend': 'Contact',
            'clamav': 'ClamAV'
        }
        self.chart_dependencies_table = {
            'keycloak': 'Keycloak',
            'openldap': 'OpenLDAP',
            'clamav': 'ClamAV',
            'brpmock': 'BRP Mock',
            'openzaak': 'Open Zaak',
            'opennotificaties': 'Open Notificaties',
            'objecten': 'Objecten',
            'objecttypen': 'Objecttypen',
            'openklant': 'Open Klant',
            'openforms': 'Open Formulieren',
            'openinwoner': 'Open Inwoner',
            'kiss-elastic': 'KISS Elastic'
        }

    def __read_chart_version(self):
        with urllib.request.urlopen(self.chart_file) as char_text:
            chart_data = yaml.safe_load(char_text)
            return { "chart_version": chart_data['version'], "app_version": chart_data['appVersion'] }

    def __build_chart_dependency_version(self, dependency_name, version):
        if dependency_name in self.chart_dependencies_table:
            return { "chart": self.chart_dependencies_table[dependency_name], "version": version }
        else:
            return { "chart": dependency_name, "version": version }

    def __read_chart_dependencies(self):
        chart_dependencies = {}
        with urllib.request.urlopen(self.chart_file) as char_text:
            chart_data = yaml.safe_load(char_text)
            for dependency in chart_data['dependencies']:
                v = self.__build_chart_dependency_version(dependency['name'], dependency['version'])
                chart_dependencies[v['chart']]=ComponentVersion(v['chart'], v['version'])
        return chart_dependencies

    def __build_component_version(self, product_name, version):
        if product_name in self.products_table:
            return { "component": self.products_table[product_name], "version": version }
        else:
            return { "component": product_name, "version": version }

    def __read_component_versions(self):
        component_version = {}
        with urllib.request.urlopen(self.values_file) as values_text:
            values_data = yaml.safe_load(values_text)
            for top_element, top_value in values_data.items():
                if isinstance(top_value, dict) and 'image' in top_value:
                    image_tag_value = top_value['image']['tag']
                    v = self.__build_component_version(f'{top_element}', image_tag_value)
                    component_version[v['component']]=ComponentVersion(v['component'], v['version'])
                elif isinstance(top_value, dict):
                    for sub_element, sub_value in top_value.items():
                        if isinstance(sub_value, dict) and 'image' in sub_value:
                            image_tag_value = sub_value['image']['tag']
                            v = self.__build_component_version(f'{top_element}.{sub_element}', image_tag_value)
                            component_version[v['component']]=ComponentVersion(v['component'], v['version'])
        return component_version

    def build(self):
        chart_version = self.__read_chart_version()
        chart_dependency_versions = self.__read_chart_dependencies()
        component_versions = self.__read_component_versions()
        return PodiumdVersion(self.podiumd_root_path, chart_version["chart_version"], chart_version["app_version"], chart_dependency_versions, component_versions)
