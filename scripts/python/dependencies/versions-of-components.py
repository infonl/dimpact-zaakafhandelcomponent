#  SPDX-FileCopyrightText: 2024 Lifely
#  SPDX-License-Identifier: EUPL-1.2+

from brp_version_extractor import BrpVersionExtractor
from github_tag_version_extractor import GitHubTagVersionExtractor
from postgresql_version_extractor import PostgresqlVersionExtractor

if __name__ == '__main__':
    # Extract the latest version of Haal-Centraal-BRP from the release notes
    brp_version_extractor = BrpVersionExtractor('https://brp-api.github.io/Haal-Centraal-BRP-bevragen/releasenotes')
    brp_latest_version = brp_version_extractor.get_latest_version()
    print(f'Haal-Centraal-BRP latest version: {brp_latest_version}')

    # Extract the latest version of Haal-Centraal-BRP from the GitHub tags
    open_notificaties_extractor = GitHubTagVersionExtractor('https://github.com/open-zaak/open-notificaties/tags')
    open_notificaties_latest_version = open_notificaties_extractor.get_latest_version()
    print(f'Open Notificaties latest version: {open_notificaties_latest_version}')

    # Extract the latest version of Haal-Centraal-BRP from the GitHub tags
    open_zaak_extractor = GitHubTagVersionExtractor('https://github.com/open-zaak/open-zaak/tags')
    open_zaak_latest_version = open_zaak_extractor.get_latest_version()
    print(f'Open Zaak latest version: {open_zaak_latest_version}')

    # Extract the latest version of Haal-Centraal-BRP from the GitHub tags
    open_klant_extractor = GitHubTagVersionExtractor('https://github.com/maykinmedia/open-klant/tags')
    open_klant_latest_version = open_klant_extractor.get_latest_version()
    print(f'Open Klant latest version: {open_klant_latest_version}')

    # Extract the latest version of Open Formulieren the GitHub tags
    open_formulieren_extractor = GitHubTagVersionExtractor('https://github.com/open-formulieren/open-forms/tags')
    open_formulieren_latest_version = open_formulieren_extractor.get_latest_version()
    print(f'Open Formulieren latest version: {open_formulieren_latest_version}')

    # Extract the latest version of Objecten API from the GitHub tags
    open_objecten_extractor = GitHubTagVersionExtractor('https://github.com/maykinmedia/objects-api/tags')
    open_objecten_latest_version = open_objecten_extractor.get_latest_version()
    print(f'Objecten API latest version: {open_objecten_latest_version}')

    # Extract the latest version of Object Types API from the GitHub tags
    open_objecttypes_extractor = GitHubTagVersionExtractor('https://github.com/maykinmedia/objecttypes-api/tags')
    open_objecttypes_latest_version = open_objecttypes_extractor.get_latest_version()
    print(f'Object Types API latest version: {open_objecttypes_latest_version}')

    # Extract the latest version of Object Types API from the GitHub tags
    open_objecttypes_extractor = GitHubTagVersionExtractor('https://github.com/Klantinteractie-Servicesysteem/.github/tags','v')
    open_objecttypes_latest_version = open_objecttypes_extractor.get_latest_version()
    print(f'KISS latest version: v{open_objecttypes_latest_version}')

    # Extract the latest version of Keycloak from the GitHub tags
    keycloak_extractor = GitHubTagVersionExtractor('https://github.com/keycloak/keycloak/tags')
    keycloak_latest_version = keycloak_extractor.get_latest_version()
    print(f'Keycloak latest version: {keycloak_latest_version}')

    # Extract the latest version of Clamav from the GitHub tags
    clamav_extractor = GitHubTagVersionExtractor('https://github.com/Cisco-Talos/clamav/tags','clamav-')
    clamav_latest_version = clamav_extractor.get_latest_version()
    print(f'ClamAV latest version: clamav-{clamav_latest_version}')

    # Extract the latest version of Postgresql from it's release page
    postgresql_extractor = PostgresqlVersionExtractor('https://www.postgresql.org/docs/release/')
    postgresql_latest_version = postgresql_extractor.get_latest_version()
    print(f'Postgresql latest version: {postgresql_latest_version}')
