/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.zac.util.JsonbUtil.JSONB
import nl.info.zac.app.bag.model.BagObjectType
import nl.info.zac.app.bag.model.RestAdresseerbaarObject
import nl.info.zac.app.bag.model.RestBagAdres
import nl.info.zac.app.bag.model.RestBagObject
import nl.info.zac.app.bag.model.RestNummeraanduiding
import nl.info.zac.app.bag.model.RestOpenbareRuimte
import nl.info.zac.app.bag.model.RestPand
import nl.info.zac.app.bag.model.RestWoonplaats
import java.lang.reflect.Type

class RestBagObjectJsonbDeserializer : JsonbDeserializer<RestBagObject> {
    override fun deserialize(
        parser: JsonParser,
        deserializationContext: DeserializationContext,
        runtimeType: Type
    ): RestBagObject {
        val json = parser.`object`
        return when (BagObjectType.valueOf(json.getString("bagObjectType"))) {
            BagObjectType.ADRES -> JSONB.fromJson(json.toString(), RestBagAdres::class.java)
            BagObjectType.NUMMERAANDUIDING -> JSONB.fromJson(json.toString(), RestNummeraanduiding::class.java)
            BagObjectType.WOONPLAATS -> JSONB.fromJson(json.toString(), RestWoonplaats::class.java)
            BagObjectType.PAND -> JSONB.fromJson(json.toString(), RestPand::class.java)
            BagObjectType.OPENBARE_RUIMTE -> JSONB.fromJson(json.toString(), RestOpenbareRuimte::class.java)
            BagObjectType.ADRESSEERBAAR_OBJECT -> JSONB.fromJson(json.toString(), RestAdresseerbaarObject::class.java)
        }
    }
}
