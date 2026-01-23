#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+
#

class ComponentVersion:
    """
    Component version information data object.
    """

    def __init__(self, name, version):
        self.name = name
        self.version = version

    def __str__(self):
        return f'{self.name}: {self.version}'

    def __lt__(self, other):
        return self.name < other.name
