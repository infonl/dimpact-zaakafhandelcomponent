/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.ws.rs.ext.ContextResolver
import nl.info.client.zgw.zrc.jsonb.DeleteGeoJSONGeometryJsonbSerializer
import nl.info.client.zgw.zrc.jsonb.RolJsonbDeserializer
import nl.info.client.zgw.zrc.jsonb.ZaakObjectJsonbDeserializer

class JsonbConfiguration : ContextResolver<Jsonb> {
    override fun getContext(type: Class<*>): Jsonb = jsonb

    private val jsonb: Jsonb = JsonbBuilder.create(
        JsonbConfig()
            .withDeserializers(
                RolJsonbDeserializer(),
                ZaakObjectJsonbDeserializer(),
                URIJsonbDeserializer()
            )
            .withSerializers(
                DeleteGeoJSONGeometryJsonbSerializer()
            )
    )
}
