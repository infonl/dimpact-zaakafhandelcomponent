/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

enum class FunctionalRole(val value: String) {
    BEHANDELAAR("behandelaar"),
    DOMEIN_ELK_ZAAKTYPE("domein_elk_zaaktype"),
    COORDINATOR("coordinator"),
    ZAAKAFHANDELCOMPONENT_USER("zaakafhandelcomponent_user"),
    BEHEERDER("beheerder"),
    RECORDMANAGER("recordmanager"),
    RAADPLEGER("raadpleger")
}
