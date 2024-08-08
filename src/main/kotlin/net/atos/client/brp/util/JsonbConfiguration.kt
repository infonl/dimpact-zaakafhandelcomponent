/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.ws.rs.ext.ContextResolver

class JsonbConfiguration : ContextResolver<Jsonb> {
    private val jsonb: Jsonb

    init {
        JsonbConfig().withDeserializers(PersonenQueryResponseJsonbDeserializer()).let {
            jsonb = JsonbBuilder.create(it)
        }
    }

    override fun getContext(type: Class<*>): Jsonb = jsonb
}
