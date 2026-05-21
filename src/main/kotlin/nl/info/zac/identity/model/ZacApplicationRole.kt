/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

/**
 * Defines those ZAC application roles that require specific handling in the application code.
 * ZAC defines other application roles that are not listed here, but those roles do not require
 * specific handling in the application code.
 * For an overview of all ZAC application roles, see: [rollen.rego](../../../../resources/policies/rollen.rego).
 */
enum class ZacApplicationRole(val value: String) {
    BEHEERDER("beheerder"),
    BEHANDELAAR("behandelaar")
}
