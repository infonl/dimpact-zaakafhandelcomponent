/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

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
data class UserIdentificatie(
    @field:NotNull
    val bsnNummer: String
) : BetrokkeneIdentificatie()

@JsonTypeName("VN")
data class VestigingIdentificatie(
    @field:NotNull
    val kvkNummer: String,

    @field:NotBlank
    val vestigingsnummer: String
) : BetrokkeneIdentificatie()

@JsonTypeName("RSIN")
data class RsinIdentificatie(
    @field:NotNull
    val rsinNummer: String
) : BetrokkeneIdentificatie()
