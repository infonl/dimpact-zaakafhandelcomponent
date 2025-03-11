/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

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
import nl.info.zac.exception.InputValidationFailedException
import java.lang.reflect.Type

class PersonenQueryResponseJsonbDeserializer : JsonbDeserializer<PersonenQueryResponse> {
    companion object {
        const val RAADPLEEG_MET_BURGERSERVICENUMMER = "RaadpleegMetBurgerservicenummer"
        const val ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM = "ZoekMetGeslachtsnaamEnGeboortedatum"
        const val ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING = "ZoekMetNaamEnGemeenteVanInschrijving"
        const val ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE = "ZoekMetNummeraanduidingIdentificatie"
        const val ZOEK_MET_POSTCODE_EN_HUISNUMMER = "ZoekMetPostcodeEnHuisnummer"
        const val ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING =
            "ZoekMetStraatHuisnummerEnGemeenteVanInschrijving"

        private val JSONB = JsonbBuilder.create(
            JsonbConfig()
                .withPropertyVisibilityStrategy(FieldPropertyVisibilityStrategy())
                .withDeserializers(
                    AbstractDatumJsonbDeserializer(),
                    AbstractVerblijfplaatsJsonbDeserializer()
                )
        )
    }

    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext,
        rtType: Type
    ): PersonenQueryResponse =
        parser.getObject().let {
            when (val type = it.getString("type")) {
                RAADPLEEG_MET_BURGERSERVICENUMMER -> JSONB.fromJson(
                    it.toString(),
                    RaadpleegMetBurgerservicenummerResponse::class.java
                )
                ZOEK_MET_GESLACHTSNAAM_EN_GEBOORTEDATUM -> JSONB.fromJson(
                    it.toString(),
                    ZoekMetGeslachtsnaamEnGeboortedatumResponse::class.java
                )
                ZOEK_MET_NAAM_EN_GEMEENTE_VAN_INSCHRIJVING -> JSONB.fromJson(
                    it.toString(),
                    ZoekMetNaamEnGemeenteVanInschrijvingResponse::class.java
                )
                ZOEK_MET_NUMMERAANDUIDING_IDENTIFICATIE -> JSONB.fromJson(
                    it.toString(),
                    ZoekMetNummeraanduidingIdentificatieResponse::class.java
                )
                ZOEK_MET_POSTCODE_EN_HUISNUMMER -> JSONB.fromJson(
                    it.toString(),
                    ZoekMetPostcodeEnHuisnummerResponse::class.java
                )
                ZOEK_MET_STRAAT_HUISNUMMER_EN_GEMEENTE_VAN_INSCHRIJVING -> JSONB.fromJson(
                    it.toString(),
                    ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse::class.java
                )
                else -> throw InputValidationFailedException(message = "Unsupported type: '$type'")
            }
        }
}
