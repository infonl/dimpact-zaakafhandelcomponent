#!/usr/bin/env python

#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

from podiumd_version_builder import PodiumdVersionBuilder
from podiumd_version_comparator import PodiumdVersionComparator
from argparse import ArgumentParser

def __build_podiumd_version(v = None, b = None):
    """
    Build PodiumD version information for the provided version, and print it.

    :param v: Version to retrieve and print component version information for (optional)
    :param b: Branch to retrieve information from (optional)
    :return: the PodiumD version information
    """

    if v is None:
        print('# PodiumD Latest version')
    else:
        print(f'# PodiumD {v} version')

    podiumd_version = PodiumdVersionBuilder(v,b).build()
    print(podiumd_version)
    return podiumd_version


def __print_podiumd_versions_and_compare_latest_with(old_version, new_version = None, new_branch = None):
    """
    Print podiumd version information for both versions provided and compare these versions to get information about the
    difference between the used components in these PodiumD versions.

    :param old_version: version to start comparison from
    :param new_version: version to start comparison to (optional)
    :param new_branch: branch to use for new version comparison on, if there's no tag (optional)
    """
    new_podiumd = __build_podiumd_version(new_version, new_branch)
    old_podiumd = __build_podiumd_version(old_version)

    print('# Compare PodiumD versions')
    PodiumdVersionComparator(old_podiumd, new_podiumd).print_version_updated_table()

def __main():
    # Create the parser
    parser = ArgumentParser(description="Script for comparing PodiumD version details between specific versions")

    # Add arguments
    parser.add_argument('-o', '--old_version', type=str, help='Version to base comparison on', required=True)
    parser.add_argument('-n', '--new_version', type=str, help='Version to compare with. Defaults to the latest on main.', required=False)
    parser.add_argument('-b', '--new_version_branch', type=str, help='Branch to comparison on. Defaults to the main branch.', required=False)

    # Parse the arguments
    args = parser.parse_args()

    # Use the arguments
    __print_podiumd_versions_and_compare_latest_with(args.old_version, args.new_version, args.new_version_branch)

if __name__ == '__main__':
    __main()
