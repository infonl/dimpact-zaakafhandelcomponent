#!/usr/bin/env python

#  SPDX-FileCopyrightText: 2024 INFO.nl
#  SPDX-License-Identifier: EUPL-1.2+

from kvk_version_extractor import KvkVersionExtractor
from brp_version_extractor import BrpVersionExtractor
from github_tag_version_extractor import GitHubTagVersionExtractor
from postgresql_version_extractor import PostgresqlVersionExtractor
from docker_version_extractor import DockerHubVersionExtractor
from latest_version_printer import print_latest_version


def print_versions_of_components():
    """
    Print the version of the following components:

    - Haal-Centraal-BRP     from https://brp-api.github.io/Haal-Centraal-BRP-bevragen/releasenotes
    - Open Notificaties     from https://github.com/open-zaak/open-notificaties/tags
    - Open Zaak             from https://github.com/open-zaak/open-zaak/tags
    - Open Klant            from https://github.com/maykinmedia/open-klant/tags
    - Open Formulieren      from https://github.com/open-formulieren/open-forms/tags
    - Open Object           from https://github.com/maykinmedia/open-object/tags
    - Open Inwoner          from https://github.com/maykinmedia/open-inwoner/tags
    - KISS                  from https://github.com/Klantinteractie-Servicesysteem/KISS-frontend/releases
    - Keycloak              from https://github.com/keycloak/keycloak/tags
    - ClamAV                from https://github.com/Cisco-Talos/clamav/tags
    - Postgresql            from https://www.postgresql.org/docs/release/
    - Infinispan            from https://github.com/infinispan/infinispan/tags
    """

    print_latest_version('KvK Zoeken API latest version', lambda: KvkVersionExtractor(
        'https://developers.kvk.nl/documentation/release-notes/zoeken-api').get_latest_version())
    print_latest_version('KvK Basisprofiel API latest version', lambda: KvkVersionExtractor(
        'https://developers.kvk.nl/documentation/release-notes/basisprofiel-api').get_latest_version())
    print_latest_version('KvK Vestigingsprofiel API latest version', lambda: KvkVersionExtractor(
        'https://developers.kvk.nl/documentation/release-notes/vestigingsprofiel-api').get_latest_version())
    print_latest_version('Haal-Centraal-BRP latest version', lambda: BrpVersionExtractor(
        'https://brp-api.github.io/Haal-Centraal-BRP-bevragen/releasenotes').get_latest_version())
    print_latest_version('Open Notificaties latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/open-zaak/open-notificaties/tags').get_latest_version())
    print_latest_version('Open Zaak latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/open-zaak/open-zaak/tags').get_latest_version())
    print_latest_version('Open Klant latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/maykinmedia/open-klant/tags').get_latest_version())
    print_latest_version('Open Formulieren latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/open-formulieren/open-forms/tags').get_latest_version())
    print_latest_version('Open Object latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/maykinmedia/open-object/tags').get_latest_version())
    print_latest_version('Open Inwoner latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/maykinmedia/open-inwoner/tags', 'v').get_latest_version())
    print_latest_version('KISS latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/Klantinteractie-Servicesysteem/KISS-frontend/tags', 'v').get_latest_version())
    print_latest_version('Zaakafhandelcomponent latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/infonl/dimpact-zaakafhandelcomponent/tags', 'v', hotfix_suffix=True).get_latest_version())
    print_latest_version('Keycloak latest version', lambda: GitHubTagVersionExtractor(
        'https://github.com/keycloak/keycloak/tags').get_latest_version())
    print_latest_version('ClamAV latest version', lambda: 'clamav-' + str(GitHubTagVersionExtractor(
        'https://github.com/Cisco-Talos/clamav/tags', 'clamav-').get_latest_version()))
    print_latest_version('Postgresql latest version', lambda: PostgresqlVersionExtractor(
        'https://www.postgresql.org/docs/release/').get_latest_version())
    print_latest_version('Infinispan latest version', lambda: DockerHubVersionExtractor(
        'infinispan/server').get_latest_version())


if __name__ == '__main__':
    print_versions_of_components()
