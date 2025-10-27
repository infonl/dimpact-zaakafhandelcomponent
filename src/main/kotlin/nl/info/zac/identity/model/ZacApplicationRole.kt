/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

enum class ZacApplicationRole(val value: String) {
    @Deprecated(
        message = "Not used in new PABC based IAM architecture"
    )
    DOMEIN_ELK_ZAAKTYPE("domein_elk_zaaktype"),
    BEHEERDER("beheerder"),
    BEHANDELAAR("behandelaar"),
    COORDINATOR("coordinator"),
    RAADPLEGER("raadpleger"),
    RECORDMANAGER("recordmanager"),
}
