/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.TypeOpenbareRuimte

class TypeOpenbareRuimteEnumAdapter : JsonbAdapter<TypeOpenbareRuimte, String> {
    override fun adaptToJson(typeOpenbareRuimte: TypeOpenbareRuimte): String = typeOpenbareRuimte.toString()
    override fun adaptFromJson(json: String): TypeOpenbareRuimte = TypeOpenbareRuimte.fromValue(json)
}
