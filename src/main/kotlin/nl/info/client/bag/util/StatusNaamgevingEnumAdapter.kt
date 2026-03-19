/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.client.bag.model.generated.StatusNaamgeving

class StatusNaamgevingEnumAdapter : JsonbAdapter<StatusNaamgeving, String> {
    override fun adaptToJson(statusNaamgeving: StatusNaamgeving): String = statusNaamgeving.toString()
    override fun adaptFromJson(json: String): StatusNaamgeving = StatusNaamgeving.fromValue(json)
}
