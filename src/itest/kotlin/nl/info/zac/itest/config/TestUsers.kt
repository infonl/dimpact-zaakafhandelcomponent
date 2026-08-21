/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

// These users are part of one or more of the Keycloak groups and functional roles.
// For these functional roles authorisation mappings to zaaktypes (grouped by domains) and application roles exist in the PABC.
/**
 * A raadpleger in domein test 1.
 */
val RAADPLEGER_1 = TestUser(
    username = "raadpleger1",
    password = "raadpleger1",
    displayName = "Test Raadpleger 1",
    email = "raadpleger-test-1@example.com"
)

/**
 * A raadpleger in domein test 2.
 */
val RAADPLEGER_2 = TestUser(
    username = "raadpleger2",
    password = "raadpleger2",
    displayName = "Test Raadpleger 2",
    email = "raadpleger-test-2@example.com"
)

/**
 * A behandelaar in domein test 1.
 */
val BEHANDELAAR_1 = TestUser(
    username = "behandelaar1",
    password = "behandelaar1",
    displayName = "Test Behandelaar 1",
    email = "behandelaar-test-1@example.com"
)

/**
 * A behandelaar in domein test 2. Does not have the brp_zoeken role
 */
val BEHANDELAAR_2 = TestUser(
    username = "behandelaar2",
    password = "behandelaar2",
    displayName = "Test Behandelaar 2",
    email = "behandelaar-test-2@example.com"
)

/**
 * A coordinator in domein test 1.
 */
val COORDINATOR_1 = TestUser(
    username = "coordinator1",
    password = "coordinator1",
    displayName = "Test Coordinator 1",
    email = "coordinator-test-1@example.com"
)

/**
 * A coordinator in domein test 2.
 */
val COORDINATOR_2 = TestUser(
    username = "coordinator2",
    password = "coordinator2",
    displayName = "Test Coordinator 2",
    email = "coordinator-test-2@example.com"
)

/**
 * A recordmanager in domein test 1.
 */
val RECORDMANAGER_1 = TestUser(
    username = "recordmanager1",
    password = "recordmanager1",
    displayName = "Test Recordmanager 1",
    email = "recordmanager-test-1@example.com"
)

/**
 * A recordmanager in domein test 2.
 */
val RECORDMANAGER_2 = TestUser(
    username = "recordmanager2",
    password = "recordmanager2",
    displayName = "Test Recordmanager 2",
    email = "recordmanager-test-2@example.com"
)

/**
 * A beheerder in all domeinen (and hence all zaaktypes).
 */
val BEHEERDER_1 = TestUser(
    username = "beheerder1",
    password = "beheerder1",
    displayName = "Test Beheerder 1",
    email = "beheerder-test-1@example.com"
)

/**
 * A raadpleger in domein test 1 and a behandelaar in domein test 2.
 */
val RAADPLEGER_EN_BEHANDELAAR_1 = TestUser(
    username = "raadplegerenbehandelaar1",
    password = "raadplegerenbehandelaar1",
    displayName = "Test Raadpleger domein 1 - behandelaar domein 2",
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
val BEHANDELAAR_LONG_NAME_TEST = TestUser(
    username = "behandelaar-long-name-test-user-1",
    password = "behandelaar-long-name-test-user-1",
    displayName = "Test Behandelaar Long Name User 1",
    email = "behandelaar-long-name-test@example.com"
)

/**
 * A behandelaar for all 'zaakspecifiek geautoriseerde' zaken for domein test 1.
 */
val ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1 = TestUser(
    username = "zaakspecifiekautorisatiebehandelaar1",
    password = "zaakspecifiekautorisatiebehandelaar1",
    displayName = "Test Zaakspecifiek Autorisatie Behandelaar 1",
    email = "zaakspecifiek-autorisatie-behandelaar-test-1@example.com"
)
