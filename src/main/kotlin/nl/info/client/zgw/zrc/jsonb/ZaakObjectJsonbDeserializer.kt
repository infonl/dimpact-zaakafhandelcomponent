/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.zgw.shared.util.JsonbUtil
import net.atos.client.zgw.zrc.model.Objecttype
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import java.lang.reflect.Type

class ZaakObjectJsonbDeserializer : JsonbDeserializer<Zaakobject> {
    @Suppress("ReturnCount")
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): Zaakobject {
        val jsonObject = parser.getObject()
        val objecttype = Objecttype.fromValue(jsonObject.getString("objectType"))
        val objecttypeOverige = jsonObject.getString("objectTypeOverige")
        return when (objecttype) {
            Objecttype.ADRES -> JsonbUtil.JSONB.fromJson(jsonObject.toString(), ZaakobjectAdres::class.java)
            Objecttype.PAND -> JsonbUtil.JSONB.fromJson(jsonObject.toString(), ZaakobjectPand::class.java)
            Objecttype.OPENBARE_RUIMTE -> JsonbUtil.JSONB.fromJson(
                jsonObject.toString(),
                ZaakobjectOpenbareRuimte::class.java
            )
            Objecttype.WOONPLAATS -> JsonbUtil.JSONB.fromJson(jsonObject.toString(), ZaakobjectWoonplaats::class.java)
            Objecttype.OVERIGE -> when (objecttypeOverige) {
                ZaakobjectProductaanvraag.OBJECT_TYPE_OVERIGE -> JsonbUtil.JSONB.fromJson(
                    jsonObject.toString(),
                    ZaakobjectProductaanvraag::class.java
                )
                ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE -> JsonbUtil.JSONB.fromJson(
                    jsonObject.toString(),
                    ZaakobjectNummeraanduiding::class.java
                )
                else -> throw ZrcRuntimeException("objectType '$objecttype' wordt niet ondersteund")
            }
            else -> throw ZrcRuntimeException("objectType '$objecttype' wordt niet ondersteund")
        }
    }
}
