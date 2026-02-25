/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.ws.rs.ext.ContextResolver

class JsonbConfiguration : ContextResolver<Jsonb> {
    private val jsonb: Jsonb = JsonbBuilder.create(
        JsonbConfig().withAdapters(
            IndicatieEnumAdapter(),
            StatusNaamgevingEnumAdapter(),
            StatusPandEnumAdapter(),
            StatusWoonplaatsEnumAdapter(),
            StatusVerblijfsobjectEnumAdapter(),
            TypeAdresseerbaarObjectEnumAdapter(),
            GebruiksdoelEnumAdapter(),
            TypeOpenbareRuimteEnumAdapter()
        )
    )

    override fun getContext(type: Class<*>): Jsonb = jsonb
}
