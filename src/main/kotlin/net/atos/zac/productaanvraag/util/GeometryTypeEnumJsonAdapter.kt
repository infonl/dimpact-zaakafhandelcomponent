/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.zac.productaanvraag.model.generated.Geometry

/**
 * JSON adapter for the [Geometry.Type] enum that matches on the enum's value instead of the enum's name.
 */
class GeometryTypeEnumJsonAdapter : JsonbAdapter<Geometry.Type, String> {
    override fun adaptToJson(value: Geometry.Type): String = value.name
    override fun adaptFromJson(s: String): Geometry.Type = Geometry.Type.fromValue(s)
}
