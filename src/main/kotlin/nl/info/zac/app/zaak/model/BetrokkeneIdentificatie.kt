/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(
    description = "Identificatie van een betrokkene",
    discriminatorProperty = "type",
    oneOf = [UserIdentificatie::class, VestigingIdentificatie::class, RsinIdentificatie::class],
    discriminatorMapping = [
        DiscriminatorMapping(value = "BSN", schema = UserIdentificatie::class),
        DiscriminatorMapping(value = "VN", schema = VestigingIdentificatie::class),
        DiscriminatorMapping(value = "RSIN", schema = RsinIdentificatie::class)
    ]
)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserIdentificatie::class, name = "BSN"),
    JsonSubTypes.Type(value = VestigingIdentificatie::class, name = "VN"),
    JsonSubTypes.Type(value = RsinIdentificatie::class, name = "RSIN")
)
sealed class BetrokkeneIdentificatie

@JsonTypeName("BSN")
@Schema(name = "BSN")
data class UserIdentificatie(
    @field:NotNull
    val bsnNummer: String
) : BetrokkeneIdentificatie()

@JsonTypeName("VN")
@Schema(name = "VN")
data class VestigingIdentificatie(
    @field:NotNull
    val kvkNummer: String,

    @field:NotBlank
    val vestigingsnummer: String
) : BetrokkeneIdentificatie()

@JsonTypeName("RSIN")
@Schema(name = "RSIN")
data class RsinIdentificatie(
    @field:NotNull
    val rsinNummer: String
) : BetrokkeneIdentificatie()
