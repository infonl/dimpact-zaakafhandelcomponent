/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.json.bind.annotation.JsonbTypeAdapter
import nl.info.client.zgw.shared.model.AbstractEnum

/**
 *
 */
@JsonbTypeAdapter(IndicatieInternExtern.Adapter::class)
enum class IndicatieInternExtern(private val value: String) : AbstractEnum {
    INTERN("intern"),

    EXTERN("extern");

    override fun toValue(): String = value

    internal class Adapter : AbstractEnum.Adapter<IndicatieInternExtern>() {
        override fun getEnums(): Array<IndicatieInternExtern> = entries.toTypedArray()
    }

    companion object {
        fun fromValue(value: String): IndicatieInternExtern = AbstractEnum.fromValue(entries.toTypedArray(), value)
    }
}
