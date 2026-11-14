/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.json.bind.annotation.JsonbTypeAdapter
import net.atos.client.zgw.shared.model.AbstractEnum

/**
 * Aanduiding van de richting van informatieobjecten van het gerelateerde INFORMATIEOBJECTTYPE bij zaken van het gerelateerde ZAAKTYPE.
 */
@JsonbTypeAdapter(Richting.Adapter::class)
enum class Richting(private val value: String) : AbstractEnum<Richting> {
    INKOMEND("inkomend"),

    INTERN("intern"),

    UITGAAND("uitgaand");

    override fun toValue(): String {
        return value
    }

    internal class Adapter : AbstractEnum.Adapter<Richting>() {
        override fun getEnums(): Array<Richting> {
            return entries.toTypedArray()
        }
    }

    companion object {
        fun fromValue(value: String): Richting {
            return AbstractEnum.fromValue(entries.toTypedArray(), value)
        }
    }
}
