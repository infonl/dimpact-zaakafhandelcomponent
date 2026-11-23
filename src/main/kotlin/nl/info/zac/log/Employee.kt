/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

/**
 * A log field that identifies an employee. [ZacLogger] writes the Keycloak user ID of the employee, never the
 * username.
 */
class Employee(val username: String) {
    override fun toString() = "Employee(username=$REDACTED_VALUE)"
}
