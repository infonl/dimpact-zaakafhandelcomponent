/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.json.bind.adapter.JsonbAdapter
import jakarta.json.bind.annotation.JsonbTypeAdapter
import org.apache.commons.lang3.NotImplementedException
import java.util.logging.Logger
import kotlin.collections.mutableMapOf

/**
 * Enumeratie die de acties bevat zoals die binnenkomen op de [NotificationReceiver].
 */
@JsonbTypeAdapter(Action.Adapter::class)
enum class Action(private val code: String, private val alternativeCode: String? = null) {
    CREATE("create"),
    READ("read", "list"),
    UPDATE("update", "partial_update"),
    DELETE("destroy");

    companion object {
        private val LOG = Logger.getLogger(Action::class.java.getName())
        private val VALUES = mutableMapOf<String, Action>()

        init {
            for (value in entries) {
                VALUES.put(value.code, value)
                if (value.alternativeCode != null) {
                    VALUES.put(value.alternativeCode, value)
                }
            }
        }

        fun fromCode(code: String): Action? =
            VALUES[code].also {
                if (it == null) {
                    LOG.warning("Unknown action: '$code'")
                }
            }
    }

    internal class Adapter : JsonbAdapter<Action, String> {
        override fun adaptToJson(action: Action): String {
            throw NotImplementedException()
        }

        override fun adaptFromJson(code: String): Action? {
            return fromCode(code)
        }
    }
}
