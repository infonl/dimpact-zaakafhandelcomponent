/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.StatusVerblijfsobject

class StatusVerblijfsobjectEnumAdapter : JsonbAdapter<StatusVerblijfsobject, String> {
    override fun adaptToJson(statusVerblijfsobject: StatusVerblijfsobject): String = statusVerblijfsobject.toString()
    override fun adaptFromJson(json: String): StatusVerblijfsobject = StatusVerblijfsobject.fromValue(json)
}
