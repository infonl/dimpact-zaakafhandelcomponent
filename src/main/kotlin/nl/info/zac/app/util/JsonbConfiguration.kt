/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.ws.rs.ext.ContextResolver
import jakarta.ws.rs.ext.Provider
import nl.info.zac.util.time.LocalDateAdapter
import nl.info.zac.util.time.ZonedDateTimeAdapter

@Provider
class JsonbConfiguration : ContextResolver<Jsonb> {
    private val jsonb: Jsonb = JsonbBuilder.create(
        JsonbConfig()
            .withAdapters(
                ZonedDateTimeAdapter(),
                LocalDateAdapter()
            ).withDeserializers(
                RestBagObjectJsonbDeserializer()
            )
    )

    override fun getContext(type: Class<*>): Jsonb = jsonb
}
