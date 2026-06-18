/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

/**
 * This enum represents the possible indicaties for a zaak.
 *
 * The order of the enum values determines the sort priority of the indicators (the highest priority indicator first)
 * Nota bene: When the order of the enum values changes, the Elasticsearch index for zaken MUST be rebuilt
 * (trigger a reindex of the ZAAK object type).
 *
 * Oh, and no more than 63 indicaties in this enum please (it needs to fit in a signed long)
 */
enum class ZaakIndicatie {
    OPSCHORTING,
    HEROPEND,
    HOOFDZAAK,
    DEELZAAK,
    VERLENGD,
    ONTVANGSTBEVESTIGING_NIET_VERSTUURD
}
