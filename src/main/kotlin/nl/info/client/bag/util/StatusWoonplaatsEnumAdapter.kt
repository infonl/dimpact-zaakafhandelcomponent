/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.StatusWoonplaats

class StatusWoonplaatsEnumAdapter : JsonbAdapter<StatusWoonplaats, String> {
    override fun adaptToJson(statusWoonplaats: StatusWoonplaats): String = statusWoonplaats.toString()
    override fun adaptFromJson(json: String): StatusWoonplaats = StatusWoonplaats.fromValue(json)
}
