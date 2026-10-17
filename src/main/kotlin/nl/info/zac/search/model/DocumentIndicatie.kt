/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

/*
 * This enum represents the possible indicaties for an informatieobject.
 *
 * The order of the enum values determines the sort priority of the indicators (the highest priority indicator first)
 * Nota bene: When the order of the enum values changes, the Solr index for zaken MUST be rebuilt.
 * Oh, and no more than 63 indicaties in this enum please (it needs to fit in a signed plong in Solr)
 */
enum class DocumentIndicatie {
    VERGRENDELD,
    ONDERTEKEND,
    BESLUIT,
    GEBRUIKSRECHT,
    VERZONDEN
}
