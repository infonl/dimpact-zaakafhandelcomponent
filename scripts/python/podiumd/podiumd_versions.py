#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

from podiumd_version_builder import PodiumdVersionBuilder
from podiumd_version_comparator import PodiumdVersionComparator

print('# PodiumD Latest version')
latest_podiumd = PodiumdVersionBuilder().build()
print(latest_podiumd)

print('# PodiumD 3.0.2 version')
three_zero_podiumd = PodiumdVersionBuilder("3.0.2").build()
print(three_zero_podiumd)

print('# Compare PodiumD versions')
PodiumdVersionComparator(latest_podiumd, three_zero_podiumd).print_version_updated_table()

# print('# PodiumD 2.1.0 version')
# two_one_podiumd = PodiumdVersionBuilder("2.1.0")
# two_one_podiumd.read_chart_version()
# two_one_podiumd.print_component_versions()

