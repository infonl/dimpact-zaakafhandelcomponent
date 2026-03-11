/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.StatusPand

class StatusPandEnumAdapter : JsonbAdapter<StatusPand, String> {
    override fun adaptToJson(statusPand: StatusPand): String = statusPand.toString()
    override fun adaptFromJson(json: String): StatusPand = StatusPand.fromValue(json)
}
