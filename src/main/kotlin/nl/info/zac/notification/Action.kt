/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.json.bind.adapter.JsonbAdapter
import jakarta.json.bind.annotation.JsonbTypeAdapter
import org.apache.commons.lang3.NotImplementedException
import java.util.logging.Logger

/**
 * Defines notification actions as handled in [NotificationReceiver].
 */
@JsonbTypeAdapter(Action.Adapter::class)
enum class Action(private val code: String, private val alternativeCode: String? = null) {
    CREATE("create"),
    READ("read", "list"),
    UPDATE("update", "partial_update"),
    DELETE("destroy");

    companion object {
        private val LOG = Logger.getLogger(Action::class.java.getName())
    }

    internal class Adapter : JsonbAdapter<Action, String> {
        override fun adaptToJson(action: Action): String {
            throw NotImplementedException()
        }

        override fun adaptFromJson(code: String): Action? =
            entries.find { it.code == code || it.alternativeCode == code }.also {
                if (it == null) {
                    LOG.warning("Unknown action: '$code'")
                }
            }
    }
}
