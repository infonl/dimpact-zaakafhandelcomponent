/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.zgw.shared.util.JsonbUtil.JSONB
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import java.lang.reflect.Type

class RolJsonbDeserializer : JsonbDeserializer<Rol<*>> {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): Rol<*> {
        val jsonObject = parser.getObject()
        val betrokkenetype = BetrokkeneTypeEnum.fromValue(jsonObject.getJsonString(Rol.BETROKKENE_TYPE_NAAM).string)

        return when (betrokkenetype) {
            BetrokkeneTypeEnum.VESTIGING -> JSONB.fromJson(
                jsonObject.toString(),
                RolVestiging::class.java
            )

            BetrokkeneTypeEnum.MEDEWERKER -> JSONB.fromJson(
                jsonObject.toString(),
                RolMedewerker::class.java
            )

            BetrokkeneTypeEnum.NATUURLIJK_PERSOON -> JSONB.fromJson(
                jsonObject.toString(),
                RolNatuurlijkPersoon::class.java
            )

            BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON -> JSONB.fromJson(
                jsonObject.toString(),
                RolNietNatuurlijkPersoon::class.java
            )

            BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID -> JSONB.fromJson(
                jsonObject.toString(),
                RolOrganisatorischeEenheid::class.java
            )
        }
    }
}
