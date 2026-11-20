#  SPDX-FileCopyrightText: 2026 INFO.nl
#  SPDX-License-Identifier: EUPL-1.2+


def print_latest_version(label, get_latest_version):
    """
    Print the latest version of one component. A lookup that fails prints '?' with the error on that line, so the
    other components are still printed.
    """
    try:
        print(f'{label}: {get_latest_version()}')
    except Exception as error:
        print(f'{label}: ? ({type(error).__name__}: {error})')
