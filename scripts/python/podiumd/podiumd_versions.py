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


