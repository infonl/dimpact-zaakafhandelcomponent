/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.json.bind.annotation.JsonbTypeAdapter

/**
 * Bron API
 */
@JsonbTypeAdapter(Bron.Adapter::class)
enum class Bron(private val value: String) : AbstractEnum {

    /**
     * Autorisaties API.
     */
    AUTORISATIES_API("AC"),

    /**
     * Notificaties API.
     */
    NOTIFICATIES_API("NRC"),

    /**
     * Zaken API
     */
    ZAKEN_API("ZRC"),

    /**
     * Catalogi API
     */
    CATALOGI_API("ZTC"),

    /**
     * Documenten API
     */
    DOCUMENTEN_API("DRC"),

    /**
     * Besluiten API
     */
    BESLUITEN_API("BRC");

    companion object {
        fun fromValue(value: String): Bron = AbstractEnum.fromValue(entries.toTypedArray(), value)
    }

    override fun toValue(): String = value

    internal class Adapter : AbstractEnum.Adapter<Bron>() {
        override fun getEnums(): Array<Bron> = entries.toTypedArray()
    }
}
