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
@JsonbTypeAdapter(IndicatieInternExtern.Adapter::class)
enum class IndicatieInternExtern(private val value: String) : AbstractEnum<IndicatieInternExtern> {
    INTERN("intern"),

    EXTERN("extern");

    override fun toValue(): String {
        return value
    }

    internal class Adapter : AbstractEnum.Adapter<IndicatieInternExtern>() {
        override fun getEnums(): Array<IndicatieInternExtern> {
            return entries.toTypedArray()
        }
    }

    companion object {
        fun fromValue(value: String): IndicatieInternExtern {
            return AbstractEnum.fromValue(entries.toTypedArray(), value)
        }
    }
}
