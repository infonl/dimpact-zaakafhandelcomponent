/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.Gebruiksdoel

class GebruiksdoelEnumAdapter : JsonbAdapter<Gebruiksdoel, String> {
    override fun adaptToJson(gebruiksdoel: Gebruiksdoel): String = gebruiksdoel.toString()
    override fun adaptFromJson(json: String): Gebruiksdoel = Gebruiksdoel.fromValue(json)
}
