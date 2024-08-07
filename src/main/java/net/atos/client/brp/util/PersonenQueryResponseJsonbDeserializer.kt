/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.brp.model.generated.PersonenQueryResponse
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatumResponse
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijvingResponse
import net.atos.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatieResponse
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummerResponse
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse
import java.lang.reflect.Type

class PersonenQueryResponseJsonbDeserializer : JsonbDeserializer<PersonenQueryResponse> {
    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext,
        rtType: Type
    ): PersonenQueryResponse {
        val jsonObject = parser.getObject()
        val type = jsonObject.getString("type")
        return when (type) {
            RAADPLEEG_MET_BURGERSERVICENUMMER -> JSONB.fromJson(
                jsonObject.toString(),
                RaadpleegMetBurgerservicenummerResponse::class.java
            )

            ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM -> JSONB.fromJson(
                jsonObject.toString(),
                ZoekMetGeslachtsnaamEnGeboortedatumResponse::class.java
            )

            ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING -> JSONB.fromJson(
                jsonObject.toString(),
                ZoekMetNaamEnGemeenteVanInschrijvingResponse::class.java
            )

            ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE -> JSONB.fromJson(
                jsonObject.toString(),
                ZoekMetNummeraanduidingIdentificatieResponse::class.java
            )

            ZOEK_MET_POSTCODE_EN_HUISNUMMER -> JSONB.fromJson(
                jsonObject.toString(),
                ZoekMetPostcodeEnHuisnummerResponse::class.java
            )

            ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING -> JSONB.fromJson(
                jsonObject.toString(),
                ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse::class.java
            )

            else -> throw RuntimeException("Type '%s' wordt niet ondersteund".formatted(type))
        }
    }

    companion object {
        const val RAADPLEEG_MET_BURGERSERVICENUMMER: String = "RaadpleegMetBurgerservicenummer"

        const val ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM: String = "ZoekMetGeslachtsnaamEnGeboortedatum"

        const val ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING: String = "ZoekMetNaamEnGemeenteVanInschrijving"

        const val ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE: String = "ZoekMetNummeraanduidingIdentificatie"

        const val ZOEK_MET_POSTCODE_EN_HUISNUMMER: String = "ZoekMetPostcodeEnHuisnummer"

        const val ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING: String =
            "ZoekMetStraatHuisnummerEnGemeenteVanInschrijving"

        private val JSONB: Jsonb = JsonbBuilder.create(
            JsonbConfig()
                .withPropertyVisibilityStrategy(FieldPropertyVisibilityStrategy())
                .withDeserializers(
                    AbstractDatumJsonbDeserializer(),
                    AbstractVerblijfplaatsJsonbDeserializer()
                )
        )
    }
}
