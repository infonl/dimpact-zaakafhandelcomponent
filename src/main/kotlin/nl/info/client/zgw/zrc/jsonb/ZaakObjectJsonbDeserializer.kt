/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.zgw.shared.util.JsonbUtil.JSONB
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.lang.reflect.Type

class ZaakObjectJsonbDeserializer : JsonbDeserializer<Zaakobject> {
    @Suppress("ReturnCount")
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): Zaakobject {
        val jsonObject = parser.getObject()
        val objecttype = ObjectTypeEnum.fromValue(jsonObject.getString("objectType"))
        val objecttypeOverige = jsonObject.getString("objectTypeOverige")
        return when (objecttype) {
            ObjectTypeEnum.ADRES -> JSONB.fromJson(
                jsonObject.toString(),
                ZaakobjectAdres::class.java
            )
            ObjectTypeEnum.PAND -> JSONB.fromJson(
                jsonObject.toString(),
                ZaakobjectPand::class.java
            )
            ObjectTypeEnum.OPENBARE_RUIMTE -> JSONB.fromJson(
                jsonObject.toString(),
                ZaakobjectOpenbareRuimte::class.java
            )
            ObjectTypeEnum.WOONPLAATS -> JSONB.fromJson(
                jsonObject.toString(),
                ZaakobjectWoonplaats::class.java
            )
            ObjectTypeEnum.OVERIGE -> when (objecttypeOverige) {
                ZaakobjectProductaanvraag.OBJECT_TYPE_OVERIGE -> JSONB.fromJson(
                    jsonObject.toString(),
                    ZaakobjectProductaanvraag::class.java
                )
                ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE -> JSONB.fromJson(
                    jsonObject.toString(),
                    ZaakobjectNummeraanduiding::class.java
                )
                else -> throw ZrcRuntimeException("objectType '$objecttype' wordt niet ondersteund")
            }
            else -> throw ZrcRuntimeException("objectType '$objecttype' wordt niet ondersteund")
        }
    }
}
