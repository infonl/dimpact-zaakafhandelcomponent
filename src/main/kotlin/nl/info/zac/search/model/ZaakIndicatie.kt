/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

/**
 * This enum represents the possible indicaties for a zaak.
 *
 * The order of the enum values determines the sort priority of the indicators (the highest priority indicator first)
 * Nota bene: When the order of the enum values changes, the Solr index for zaken MUST be rebuilt.
 * To automatically rebuild the Solr index, please add a new ZAC Solr schema version to [net.atos.zac.solr.schema]
 * and configure it accordingly.
 * See the 'Managing Solr' developer documentation for more information.
 *
 * Oh, and no more than 63 indicaties in this enum please (it needs to fit in a signed plong in Solr)
 */
enum class ZaakIndicatie {
    OPSCHORTING,
    HEROPEND,
    HOOFDZAAK,
    DEELZAAK,
    VERLENGD
}
