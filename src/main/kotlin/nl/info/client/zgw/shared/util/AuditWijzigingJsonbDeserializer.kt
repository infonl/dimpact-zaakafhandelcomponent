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
        val wijzigingenObject = parser.value as? JsonObject ?: return null
        val waardeObject = when {
            !wijzigingenObject.isNull("oud") -> wijzigingenObject.getJsonObject("oud")
            !wijzigingenObject.isNull("nieuw") -> wijzigingenObject.getJsonObject("nieuw")
            else -> null
        }
        return waardeObject?.let {
            JSONB.fromJson(
                wijzigingenObject.toString(),
                ObjectType.getObjectType(it.getJsonString("url").string).auditClass
            )
        }
    }
}
