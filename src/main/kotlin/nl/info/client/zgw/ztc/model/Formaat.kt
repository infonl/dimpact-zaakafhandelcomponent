/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.json.bind.annotation.JsonbTypeAdapter
import net.atos.client.zgw.shared.model.AbstractEnum

/**
 *
 */
@JsonbTypeAdapter(Formaat.Adapter::class)
enum class Formaat(private val value: String) : AbstractEnum<Formaat> {
    TEKST("tekst"),

    GETAL("getal"),

    DATUM("datum"),

    DATUM_TIJD("datum_tijd");

    override fun toValue(): String {
        return value
    }

    internal class Adapter : AbstractEnum.Adapter<Formaat>() {
        override fun getEnums(): Array<Formaat> {
            return entries.toTypedArray()
        }
    }

    companion object {
        fun fromValue(value: String): Formaat {
            return AbstractEnum.fromValue(entries.toTypedArray(), value)
        }
    }
}
