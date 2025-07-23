/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.model

import jakarta.json.bind.annotation.JsonbTypeSerializer
import jakarta.json.bind.serializer.JsonbSerializer
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator

/**
 * The company type as specified by the KVK ZOEKEN API.
 * Used in requests to the KVK ZOEKEN API only.
 */
@JsonbTypeSerializer(BedrijfTypeSerializer::class)
enum class BedrijfType(val value: String) {
    HOOFDVESTIGING("hoofdvestiging"),
    NEVENVESTIGING("nevenvestiging"),
    RECHTSPERSOON("rechtspersoon")
}

class BedrijfTypeSerializer : JsonbSerializer<BedrijfType> {
    override fun serialize(bedrijfType: BedrijfType, generator: JsonGenerator, ctx: SerializationContext) {
        generator.write(bedrijfType.value)
    }
}

