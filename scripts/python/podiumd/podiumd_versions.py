#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

from podiumd_version_builder import PodiumdVersionBuilder
from podiumd_version_comparator import PodiumdVersionComparator

def build_podiumd_version(v = None):
    """
    Build PodiumD version information for the provided version, and print it.

    :param v: Version to retrieve and print component version information for.
    :return: the PodiumD version information
    """

    if v is None:
        print('# PodiumD Latest version')
    else:
        print(f'# PodiumD {v} version')

    podiumd_version = PodiumdVersionBuilder(v).build()
    print(podiumd_version)
    return podiumd_version


def print_podiumd_versions_and_compare_latest_with(old_version, new_version = None):
    """
    Print podiumd version information for both versions provided and compare these versions to get information about the
    difference between the used components in these PodiumD versions.

    :param old_version: version to start comparison from
    :param new_version: version to start comparison to, or None for latest version
    """
    new_podiumd = build_podiumd_version(new_version)
    old_podiumd = build_podiumd_version(old_version)

    print('# Compare PodiumD versions')
    PodiumdVersionComparator(old_podiumd, new_podiumd).print_version_updated_table()

if __name__ == '__main__':
    print_podiumd_versions_and_compare_latest_with("3.0.2")
