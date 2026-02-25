/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject

class TypeAdresseerbaarObjectEnumAdapter : JsonbAdapter<TypeAdresseerbaarObject, String> {
    override fun adaptToJson(typeAdresseerbaarObject: TypeAdresseerbaarObject): String = typeAdresseerbaarObject.toString()
    override fun adaptFromJson(json: String): TypeAdresseerbaarObject = TypeAdresseerbaarObject.fromValue(json)
}
