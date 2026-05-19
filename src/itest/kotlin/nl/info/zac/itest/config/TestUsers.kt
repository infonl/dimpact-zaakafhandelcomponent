/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

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
 * A recordmanager in domein test 1.
 */
val RECORDMANAGER_1 = TestUser(
    username = "recordmanager1newiam",
    password = "recordmanager1newiam",
    displayName = "Test Recordmanager 1 - new IAM",
    email = "recordmanager-test-1@example.com"
)

/**
 * A recordmanager in domein test 2.
 */
val RECORDMANAGER_2 = TestUser(
    username = "recordmanager2newiam",
    password = "recordmanager2newiam",
    displayName = "Test Recordmanager 2 - new IAM",
    email = "recordmanager-test-2@example.com"
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
val PABC_ADMIN = TestUser(
    username = "pabcadmin",
    password = "pabcadmin",
    displayName = "PABC Admin"
)
val BEHANDELAAR_INACTIVE_GROUP_1 = TestUser(
    username = "behandelaar1inactivegroup",
    password = "behandelaar1inactivegroup",
    displayName = "Test Behandelaar 1 - inactive group",
    email = "behandelaar-inactive-group-test-1@example.com"
)
