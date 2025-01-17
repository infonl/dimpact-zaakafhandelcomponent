#
#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+
#
class VersionData:
    def __init__(self, version, url):
        self.version = version
        self.url = url
        parts = self.version.split('.')
        self.major = 0
        self.minor = 0
        self.patch = 0
        self.sub = self.version
        if len(parts) > 0 and parts[0].isdigit():
            self.major = int(parts[0])
            self.sub = self.sub.replace(str(self.major), '', 1).lstrip('.-')
            if len(parts) > 1 and parts[1].isdigit():
                self.minor = int(parts[1])
                self.sub = self.sub.replace(str(self.minor), '', 1).lstrip('.-')
                if len(parts) > 2 and parts[2].isdigit():
                    self.patch = int(parts[2])
                    self.sub = self.sub.replace(str(self.patch), '', 1).lstrip('.-')

    def __str__(self):
        return f'{self.version} - {self.url}'

    def __eq__(self, other):
        return self.version == other.version and self.url == other.url

    def __lt__(self, other):
        if self.major < other.major:
            return True
        elif self.major == other.major:
            if self.minor < other.minor:
                return True
            elif self.minor == other.minor:
                if self.patch < other.patch:
                    return True
        return False

    def __gt__(self, other):
        if self.major > other.major:
            return True
        elif self.major == other.major:
            if self.minor > other.minor:
                return True
            elif self.minor == other.minor:
                if self.patch > other.patch:
                    return True
        return False

