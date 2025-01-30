#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

from kvk_version_extractor import KvkVersionExtractor
from brp_version_extractor import BrpVersionExtractor
from github_tag_version_extractor import GitHubTagVersionExtractor
from postgresql_version_extractor import PostgresqlVersionExtractor
from docker_version_extractor import DockerHubVersionExtractor


def print_versions_of_components():
    """
    Print the version of the following components:

    - Haal-Centraal-BRP     from https://brp-api.github.io/Haal-Centraal-BRP-bevragen/releasenotes
    - Open Notificaties     from https://github.com/open-zaak/open-notificaties/tags
    - Open Zaak             from https://github.com/open-zaak/open-zaak/tags
    - Open Klant            from https://github.com/maykinmedia/open-klant/tags
    - Open Formulieren      from https://github.com/open-formulieren/open-forms/tags
    - Objecten API          from https://github.com/maykinmedia/objects-api/tags
    - Object Types API      from https://github.com/maykinmedia/objecttypes-api/tags
    - Open Inwoner          from https://github.com/maykinmedia/open-inwoner/tags
    - KISS                  from https://github.com/Klantinteractie-Servicesysteem/KISS-frontend/releases
    - Keycloak              from https://github.com/keycloak/keycloak/tags
    - ClamAV                from https://github.com/Cisco-Talos/clamav/tags
    - Postgresql            from https://www.postgresql.org/docs/release/
    - Infinispan            from https://github.com/infinispan/infinispan/tags
    """

    # KvK zoeken API
    kvk_zoeken_api_version = KvkVersionExtractor(
        'https://developers.kvk.nl/documentation/release-notes/zoeken-api').get_latest_version()
    print(f'KvK Zoeken API latest version: {kvk_zoeken_api_version}')

    # KvK Basisprofiel API
    kvk_basisprofiel_api_version = KvkVersionExtractor(
        'https://developers.kvk.nl/documentation/release-notes/basisprofiel-api').get_latest_version()
    print(f'KvK Basisprofiel API latest version: {kvk_basisprofiel_api_version}')

    # KvK vestigingsprofiel API
    kvk_vestigingsprofiel_api_version = KvkVersionExtractor(
        'https://developers.kvk.nl/documentation/release-notes/vestigingsprofiel-api').get_latest_version()
    print(f'KvK Vestigingsprofiel API latest version: {kvk_vestigingsprofiel_api_version}')

    # Extract the latest version of Haal-Centraal-BRP from the release notes
    brp_latest_version = BrpVersionExtractor(
        'https://brp-api.github.io/Haal-Centraal-BRP-bevragen/releasenotes').get_latest_version()
    print(f'Haal-Centraal-BRP latest version: {brp_latest_version}')

    # Extract the latest version of Haal-Centraal-BRP from the GitHub tags
    open_notificaties_latest_version = GitHubTagVersionExtractor(
        'https://github.com/open-zaak/open-notificaties/tags').get_latest_version()
    print(f'Open Notificaties latest version: {open_notificaties_latest_version}')

    # Extract the latest version of Haal-Centraal-BRP from the GitHub tags
    open_zaak_latest_version = GitHubTagVersionExtractor(
        'https://github.com/open-zaak/open-zaak/tags').get_latest_version()
    print(f'Open Zaak latest version: {open_zaak_latest_version}')

    # Extract the latest version of Haal-Centraal-BRP from the GitHub tags
    open_klant_latest_version = GitHubTagVersionExtractor(
        'https://github.com/maykinmedia/open-klant/tags').get_latest_version()
    print(f'Open Klant latest version: {open_klant_latest_version}')

    # Extract the latest version of Open Formulieren the GitHub tags
    open_formulieren_latest_version = GitHubTagVersionExtractor(
        'https://github.com/open-formulieren/open-forms/tags').get_latest_version()
    print(f'Open Formulieren latest version: {open_formulieren_latest_version}')

    # Extract the latest version of Objecten API from the GitHub tags
    open_objecten_latest_version = GitHubTagVersionExtractor(
        'https://github.com/maykinmedia/objects-api/tags').get_latest_version()
    print(f'Objecten API latest version: {open_objecten_latest_version}')

    # Extract the latest version of Object Types API from the GitHub tags
    open_objecttypes_latest_version = GitHubTagVersionExtractor(
        'https://github.com/maykinmedia/objecttypes-api/tags').get_latest_version()
    print(f'Object Types API latest version: {open_objecttypes_latest_version}')

    # Extract the latest version of open-inwoner from the GitHub tags
    open_inwoner_latest_version = GitHubTagVersionExtractor('https://github.com/maykinmedia/open-inwoner/tags',
                                                            'v').get_latest_version()
    print(f'Open Inwoner latest version: {open_inwoner_latest_version}')

    # Extract the latest version of KISS from the GitHub tags
    kiss_latest_version = GitHubTagVersionExtractor(
        'https://github.com/Klantinteractie-Servicesysteem/KISS-frontend/tags', 'v').get_latest_version()
    print(f'KISS latest version: {kiss_latest_version}')

    # Extract the latest version of ZAC from the GitHub tags
    zac_latest_version = GitHubTagVersionExtractor('https://github.com/infonl/dimpact-zaakafhandelcomponent/tags',
                                                   'v').get_latest_version()
    print(f'Zaakafhandelcomponent latest version: {zac_latest_version}')

    # Extract the latest version of Keycloak from the GitHub tags
    keycloak_latest_version = GitHubTagVersionExtractor(
        'https://github.com/keycloak/keycloak/tags').get_latest_version()
    print(f'Keycloak latest version: {keycloak_latest_version}')

    # Extract the latest version of Clamav from the GitHub tags
    clamav_latest_version = GitHubTagVersionExtractor('https://github.com/Cisco-Talos/clamav/tags',
                                                      'clamav-').get_latest_version()
    print(f'ClamAV latest version: clamav-{clamav_latest_version}')

    # Extract the latest version of Postgresql from it's release page
    postgresql_latest_version = PostgresqlVersionExtractor(
        'https://www.postgresql.org/docs/release/').get_latest_version()
    print(f'Postgresql latest version: {postgresql_latest_version}')

    # Extract the latest version of infinispan from the GitHub tags
    infinispan_latest_version = DockerHubVersionExtractor('infinispan/server').get_latest_version()
    print(f'Infinispan latest version: {infinispan_latest_version}')

if __name__ == '__main__':
    print_versions_of_components()
