/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION

// Note that currently all group descriptions in this file are set equal to the group name.
// This is a temporary workaround for the following Keycloak issue: https://github.com/keycloak/keycloak/issues/42851
// Once this is fixed in Keycloak, the proper descriptions as defined in the Keycloak realm import file need to be used here.

// new IAM test groups; these groups have functional roles for which mappings need to exist in the PABC
val GROUP_RAADPLEGERS_TEST_1 = TestGroup(
    name = "raadplegers-test-1",
    description = "raadplegers-test-1"
)
val GROUP_RAADPLEGERS_TEST_2 = TestGroup(
    name = "raadplegers-test-2",
    description = "raadplegers-test-2"
)
val GROUP_BEHANDELAARS_TEST_1 = TestGroup(
    name = "behandelaars-test-1",
    description = "behandelaars-test-1"
)
val GROUP_BEHANDELAARS_TEST_2 = TestGroup(
    name = "behandelaars-test-2",
    description = "behandelaars-test-2"
)
val GROUP_COORDINATORS_TEST_1 = TestGroup(
    name = "coordinators-test-1",
    description = "coordinators-test-1"
)
val GROUP_COORDINATORS_TEST_2 = TestGroup(
    name = "coordinators-test-2",
    description = "coordinators-test-2"
)
val GROUP_RECORDMANAGERS_TEST_1 = TestGroup(
    name = "recordmanagers-test-1",
    description = "recordmanagers-test-1"
)
val GROUP_RECORDMANAGERS_TEST_2 = TestGroup(
    name = "recordmanagers-test-2",
    description = "recordmanagers-test-2"
)
val GROUP_BEHEERDERS_ELK_DOMEIN = TestGroup(
    name = "beheerders-elk-domein",
    description = "beheerders-elk-domein"
)

// old IAM test groups; will be removed in the future; do not use these to test new IAM functionality
val OLD_IAM_TEST_GROUP_A = TestGroup(
    name = "test-group-a",
    description = "test-group-a"
)
val OLD_IAM_TEST_GROUP_RAADPLEGERS = TestGroup(
    name = "test-group-rp",
    description = "test-group-rp"
)
val OLD_IAM_TEST_GROUP_BEHANDELAARS = TestGroup(
    name = "test-group-bh",
    description = "test-group-bh"
)
val OLD_IAM_TEST_GROUP_COORDINATORS = TestGroup(
    name = "test-group-co",
    description = "test-group-co"
)
val OLD_IAM_TEST_GROUP_RECORD_MANAGERS = TestGroup(
    name = "test-group-rm",
    description = "test-group-rm"
)
val OLD_IAM_TEST_GROUP_FUNCTIONAL_ADMINS = TestGroup(
    name = "test-group-fb",
    description = "test-group-fb"
)
val OLD_IAM_GROUP_DOMEIN_TEST_1 = TestGroup(
    name = "test-group-domein-test-1",
    description = "test-group-domein-test-1"
)
val OLD_IAM_GROUP_DOMEIN_TEST_2 = TestGroup(
    name = "test-group-domein-test-2",
    description = "test-group-domein-test-2"
)

// group constants that switch between old and new IAM test groups based on the PABC feature flag value
val RAADPLEGERS_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) GROUP_RAADPLEGERS_TEST_1 else OLD_IAM_TEST_GROUP_RAADPLEGERS
val BEHANDELAARS_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) GROUP_BEHANDELAARS_TEST_1 else OLD_IAM_TEST_GROUP_BEHANDELAARS
