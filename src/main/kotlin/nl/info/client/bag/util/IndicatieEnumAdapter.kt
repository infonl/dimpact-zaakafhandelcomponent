/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.Indicatie

class IndicatieEnumAdapter : JsonbAdapter<Indicatie, String> {
    override fun adaptToJson(indicatie: Indicatie): String = indicatie.toString()
    override fun adaptFromJson(json: String): Indicatie = Indicatie.fromValue(json)
}
