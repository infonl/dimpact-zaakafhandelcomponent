/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION

// New IAM test users
// These users are part of one or more of the new IAM Keycloak groups and functional roles.
// For these functional roles authorisation mappings to zaaktypes (grouped by domains) and application roles exist in the PABC.
/**
 * A raadpleger in domein test 1.
 */
val RAADPLEGER_1 = TestUser(
    username = "raadpleger1newiam",
    password = "raadpleger1newiam",
    displayName = "Test Raadpleger 1 - new IAM",
    email = "raadpleger-test-1@example.com"
)

/**
 * A raadpleger in domein test 2.
 */
val RAADPLEGER_2 = TestUser(
    username = "raadpleger2newiam",
    password = "raadpleger2newiam",
    displayName = "Test Raadpleger 2 - new IAM",
    email = "raadpleger-test-2@example.com"
)

/**
 * A behandelaar in domein test 1.
 */
val BEHANDELAAR_1 = TestUser(
    username = "behandelaar1newiam",
    password = "behandelaar1newiam",
    displayName = "Test Behandelaar 1 - new IAM",
    email = "behandelaar-test-1@example.com"
)

/**
 * A behandelaar in domein test 2.
 */
val BEHANDELAAR_2 = TestUser(
    username = "behandelaar2newiam",
    password = "behandelaar2newiam",
    displayName = "Test Behandelaar 2 - new IAM",
    email = "behandelaar-test-2@example.com"
)

/**
 * A coordinator in domein test 1.
 */
val COORDINATOR_1 = TestUser(
    username = "coordinator1newiam",
    password = "coordinator1newiam",
    displayName = "Test Coordinator 1 - new IAM",
    email = "coordinator-test-1@example.com"
)

/**
 * A coordinator in domein test 2.
 */
val COORDINATOR_2 = TestUser(
    username = "coordinator2newiam",
    password = "coordinator2newiam",
    displayName = "Test Coordinator 2 - new IAM",
    email = "coordinator-test-2@example.com"
)

/**
 * A beheerder in all domeinen (and hence all zaaktypes).
 */
val BEHEERDER_1 = TestUser(
    username = "beheerder1newiam",
    password = "beheerder1newiam",
    displayName = "Test Beheerder 1 - new IAM",
    email = "beheerder-test-1@example.com"
)

/**
 * A raadpleger in domein test 1 and a behandelaar in domein test 2.
 */
val RAADPLEGER_EN_BEHANDELAAR_1 = TestUser(
    username = "raadplegerenbehandelaar1newiam",
    password = "raadplegerenbehandelaar1newiam",
    displayName = "Test Raadpleger domein 1 - behandelaar domein 2 - new IAM",
    email = "raadpleger-en-behandelaar-test-1@example.com"
)
val USER_WITHOUT_ANY_ROLE = TestUser(
    username = "userwithoutanyrole",
    password = "userwithoutanyrole",
    displayName = "Test User Without Any Role"
)

// old IAM test users; will be removed in the future; do not use these to test new IAM functionality
val OLD_IAM_TEST_USER_1 = TestUser(
    username = "testuser1",
    password = "testuser1",
    displayName = "Test User1 Špëçîâl Characters",
    email = "testuser1@example.com"
)
val OLD_IAM_TEST_USER_2 = TestUser(
    username = "testuser2",
    password = "testuser2",
    // Test user 2 does not have a first name, so their full name should be equal to their last name.
    displayName = "User2"
)
val OLD_IAM_TEST_USER_DOMEIN_TEST_1 = TestUser(
    username = "testuserdomeintest1",
    password = "testuserdomeintest1",
    displayName = "Test Testuserdomeintest1"
)
val OLD_IAM_TEST_USER_DOMEIN_TEST_2 = TestUser(
    username = "testuserdomeintest2",
    password = "testuserdomeintest2",
    displayName = "Test Testuserdomeintest2",
    email = "testuserdomaintest2@example.com"
)
val OLD_IAM_FUNCTIONAL_ADMIN_1 = TestUser(
    username = "functioneelbeheerder1",
    password = "functioneelbeheerder1",
    displayName = "Test Functioneelbeheerder1"
)
val OLD_IAM_RECORD_MANAGER_1 = TestUser(
    username = "recordmanager1",
    password = "recordmanager1",
    displayName = "Test Recordmanager1"
)
val OLD_IAM_COORDINATOR_1 = TestUser(
    username = "coordinator1",
    password = "coordinator1",
    displayName = "Test Coordinator1",
    email = "coordinator1@example.com"
)
val OLD_IAM_BEHANDELAAR_1 = TestUser(
    username = "behandelaar1",
    password = "behandelaar1",
    displayName = "Test Behandelaar1",
    email = "behandelaar1@example.com"
)
val OLD_IAM_RAADPLEGER_1 = TestUser(
    username = "raadpleger1",
    password = "raadpleger1",
    displayName = "Test Raadpleger1",
    email = "raadpleger1@example.com"
)

// user constants that switch between old and new IAM test users based on the PABC feature flag value
val BEHANDELAAR_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) BEHANDELAAR_1 else OLD_IAM_BEHANDELAAR_1
val BEHANDELAAR_DOMAIN_TEST_2 = if (FEATURE_FLAG_PABC_INTEGRATION) BEHANDELAAR_2 else OLD_IAM_TEST_USER_DOMEIN_TEST_2
val BEHEERDER_ELK_ZAAKTYPE = if (FEATURE_FLAG_PABC_INTEGRATION) BEHEERDER_1 else OLD_IAM_TEST_USER_1
val COORDINATOR_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) COORDINATOR_1 else OLD_IAM_COORDINATOR_1
val RAADPLEGER_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) RAADPLEGER_1 else OLD_IAM_RAADPLEGER_1
