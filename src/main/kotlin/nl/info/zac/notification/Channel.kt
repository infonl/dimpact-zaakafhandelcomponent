/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.json.bind.adapter.JsonbAdapter
import jakarta.json.bind.annotation.JsonbTypeAdapter
import org.apache.commons.lang3.NotImplementedException
import java.util.logging.Logger
import kotlin.collections.mutableMapOf

/**
 * Defines notification [channels]((http://open-zaak.default/ref/kanalen/) ) as handled in [NotificationReceiver].
 */
@JsonbTypeAdapter(Channel.Adapter::class)
enum class Channel(private val code: String, val resourceType: Resource) {
    AUTORISATIES("autorisaties", Resource.APPLICATIE),
    BESLUITEN("besluiten", Resource.BESLUIT),
    BESLUITTYPEN("besluittypen", Resource.BESLUITTYPE),
    INFORMATIEOBJECTEN("documenten", Resource.INFORMATIEOBJECT),
    INFORMATIEOBJECTTYPEN("informatieobjecttypen", Resource.INFORMATIEOBJECTTYPE),
    OBJECTEN("objecten", Resource.OBJECT),
    ZAKEN("zaken", Resource.ZAAK),
    ZAAKTYPEN("zaaktypen", Resource.ZAAKTYPE);

    companion object {
        private val LOG = Logger.getLogger(Channel::class.java.getName())
        private val VALUES = mutableMapOf<String, Channel>()

        init {
            for (value in entries) {
                VALUES.put(value.code, value)
            }
        }

        fun fromCode(code: String): Channel? =
            VALUES[code].also {
                if (it == null) {
                    LOG.warning("Unknown channel: '$code'")
                }
            }
    }

    internal class Adapter : JsonbAdapter<Channel, String> {
        override fun adaptToJson(channel: Channel): String {
            throw NotImplementedException()
        }

        override fun adaptFromJson(code: String): Channel? {
            return fromCode(code)
        }
    }
}
