#
#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+
#
import urllib.request
import yaml

from podiumd_version import PodiumdVersion
from component_version import ComponentVersion


class PodiumdVersionBuilder:
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

    def read_chart_version(self):
        with urllib.request.urlopen(self.chart_file) as char_text:
            chart_data = yaml.safe_load(char_text)
            return { "chart_version": chart_data['version'], "app_version": chart_data['appVersion'] }

    def build_component_version(self, product_name, version):
        if product_name in self.products_table:
            return { "component": self.products_table[product_name], "version": version }
        else:
            return { "component": product_name, "version": version }

    def read_component_versions(self):
        component_version = {}
        with urllib.request.urlopen(self.values_file) as values_text:
            values_data = yaml.safe_load(values_text)
            for top_element, top_value in values_data.items():
                if isinstance(top_value, dict) and 'image' in top_value:
                    image_tag_value = top_value['image']['tag']
                    v = self.build_component_version(f'{top_element}', image_tag_value)
                    component_version[v['component']]=ComponentVersion(v['component'], v['version'])
                elif isinstance(top_value, dict):
                    for sub_element, sub_value in top_value.items():
                        if isinstance(sub_value, dict) and 'image' in sub_value:
                            image_tag_value = sub_value['image']['tag']
                            v = self.build_component_version(f'{top_element}.{sub_element}', image_tag_value)
                            component_version[v['component']]=ComponentVersion(v['component'], v['version'])
        return component_version

    def build(self):
        chart_version = self.read_chart_version()
        component_versions = self.read_component_versions()
        return PodiumdVersion(self.podiumd_root_path, chart_version["chart_version"], chart_version["app_version"], component_versions)
