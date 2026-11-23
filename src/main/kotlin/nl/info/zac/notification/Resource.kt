/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.json.bind.adapter.JsonbAdapter
import jakarta.json.bind.annotation.JsonbTypeAdapter
import nl.info.zac.log.ZacLogger
import nl.info.zac.notification.log.UnknownNotificationCode
import org.apache.commons.lang3.NotImplementedException

/**
 * Defines notification resources as handled in [NotificationReceiver].
 */
@JsonbTypeAdapter(Resource.Adapter::class)
enum class Resource(private val code: String) {
    APPLICATIE("applicatie"),
    BESLUIT("besluit"),
    BESLUITINFORMATIEOBJECT("besluitinformatieobject"),
    BESLUITTYPE("besluittype"),
    GEBRUIKSRECHTEN("gebruiksrechten"),
    INFORMATIEOBJECT("enkelvoudiginformatieobject"),
    INFORMATIEOBJECTTYPE("informatieobjecttype"),
    KLANTCONTACT("klantcontact"),
    OBJECT("object"),
    RESULTAAT("resultaat"),
    ROL("rol"),
    STATUS("status"),
    ZAAK("zaak"),
    ZAAKBESLUIT("zaakbesluit"),
    ZAAKOBJECT("zaakobject"),
    ZAAKEIGENSCHAP("zaakeigenschap"),
    ZAAKINFORMATIEOBJECT("zaakinformatieobject"),
    ZAAKTYPE("zaaktype"),

    // Used by "Abonnementen" functionality in OpenNotificaties to check if callback URL is active
    TEST("some_resource");

    companion object {
        private val LOG = ZacLogger.getLogger(Resource::class)
    }

    internal class Adapter : JsonbAdapter<Resource, String> {
        override fun adaptToJson(resource: Resource): String {
            throw NotImplementedException()
        }

        override fun adaptFromJson(code: String): Resource? =
            entries.find { it.code == code }.also {
                if (it == null) {
                    LOG.warning(UnknownNotificationCode(type = UnknownNotificationCode.Type.UNKNOWN_RESOURCE_CODE, code = code))
                }
            }
    }
}
