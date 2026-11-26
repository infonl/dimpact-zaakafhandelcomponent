/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

data object KeycloakUnreachableForEmployeeLookup : LogEvent(
    "Cannot connect to Keycloak to look up the Keycloak user ID of an employee; " +
        "employees are logged as unknown for 1 minute"
)
