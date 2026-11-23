/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.log

enum class ZaakSkipReason {
    NOT_OPEN,
    ZAAKSPECIFIEK_GEAUTORISEERD_WITHOUT_BEHANDELAAR,
    ZAAKSPECIFIEK_MEDEWERKER_ROLTYPE_NOT_FOUND,
    MULTIPLE_BEHANDELAAR_ROLES
}
