/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.zac.productaanvraag.model.generated.Betrokkene

/**
 * JSON adapter for the [Betrokkene.RolOmschrijvingGeneriek] enum that matches on the enum's value
 * instead of the enum's name.
 */
class RolOmschrijvingGeneriekEnumJsonAdapter : JsonbAdapter<Betrokkene.RolOmschrijvingGeneriek, String> {
    override fun adaptToJson(
        rolOmschrijvingGeneriek: Betrokkene.RolOmschrijvingGeneriek
    ): String = rolOmschrijvingGeneriek.toString()
    override fun adaptFromJson(value: String): Betrokkene.RolOmschrijvingGeneriek =
        Betrokkene.RolOmschrijvingGeneriek.fromValue(value)
}
