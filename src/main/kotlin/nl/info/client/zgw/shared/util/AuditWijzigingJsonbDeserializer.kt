/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.shared.util

import jakarta.json.JsonObject
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.util.JsonbUtil.JSONB
import nl.info.client.zgw.shared.model.audit.AuditWijziging
import java.lang.reflect.Type

class AuditWijzigingJsonbDeserializer : JsonbDeserializer<AuditWijziging<*>> {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): AuditWijziging<*>? {
        // Old audit trail records (e.g. inbox/detached documents) may have wijzigingen as "" instead of {}
        val value = parser.value
        if (value !is JsonObject) return null

        val waardeObject = when {
            !value.isNull("oud") -> value.getJsonObject("oud")
            !value.isNull("nieuw") -> value.getJsonObject("nieuw")
            else -> return null
        }

        val type = ObjectType.getObjectType(waardeObject.getJsonString("url").string)
        return JSONB.fromJson(value.toString(), type.auditClass)
    }
}
