/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.zac.productaanvraag.model.generated.Betrokkene

/**
 * JSON adapter for the [Betrokkene.IndicatieMachtiging] enum that matches on the enum's value instead of the enum's name.
 */
class IndicatieMachtigingEnumJsonAdapter : JsonbAdapter<Betrokkene.IndicatieMachtiging, String> {
    override fun adaptToJson(value: Betrokkene.IndicatieMachtiging): String = value.name
    override fun adaptFromJson(s: String): Betrokkene.IndicatieMachtiging = Betrokkene.IndicatieMachtiging.fromValue(s)
}
