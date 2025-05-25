/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.zgw.shared.util.JsonbUtil.JSONB
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import java.lang.reflect.Type

class RolJsonbDeserializer : JsonbDeserializer<Rol<*>> {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): Rol<*> {
        val jsonObject = parser.getObject()
        val betrokkenetype = BetrokkeneType.fromValue(jsonObject.getJsonString(Rol.BETROKKENE_TYPE_NAAM).string)

        return when (betrokkenetype) {
            BetrokkeneType.VESTIGING -> JSONB.fromJson(
                jsonObject.toString(),
                RolVestiging::class.java
            )

            BetrokkeneType.MEDEWERKER -> JSONB.fromJson(
                jsonObject.toString(),
                RolMedewerker::class.java
            )

            BetrokkeneType.NATUURLIJK_PERSOON -> JSONB.fromJson(
                jsonObject.toString(),
                RolNatuurlijkPersoon::class.java
            )

            BetrokkeneType.NIET_NATUURLIJK_PERSOON -> JSONB.fromJson(
                jsonObject.toString(),
                RolNietNatuurlijkPersoon::class.java
            )

            BetrokkeneType.ORGANISATORISCHE_EENHEID -> JSONB.fromJson(
                jsonObject.toString(),
                RolOrganisatorischeEenheid::class.java
            )
        }
    }
}
