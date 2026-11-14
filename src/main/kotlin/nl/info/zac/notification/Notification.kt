/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.json.bind.annotation.JsonbDateFormat
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.json.bind.annotation.JsonbTransient
import net.atos.client.zgw.shared.util.DateTimeUtil.DATE_TIME_FORMAT_WITH_MILLISECONDS
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime

/**
 * Notification as defined by the [ZGW Notificatie API](https://vng-realisatie.github.io/gemma-zaken/standaard/notificaties).
 */
@NoArgConstructor
@AllOpen
data class Notification(
    @set:JsonbProperty("kanaal")
    var channel: Channel,

    @set:JsonbProperty("hoofdObject")
    var mainResourceUrl: URI,

    @set:JsonbProperty("resource")
    var resource: Resource,

    @set:JsonbProperty("resourceUrl")
    var resourceUrl: URI,

    @set:JsonbProperty("actie")
    var action: Action,

    @set:JsonbProperty("aanmaakdatum")
    @JsonbDateFormat(DATE_TIME_FORMAT_WITH_MILLISECONDS)
    var creationDateTime: ZonedDateTime,

    @set:JsonbProperty("kenmerken")
    var properties: MutableMap<String, String>? = null
) {
    @JsonbTransient
    fun getResourceInfo() = ResourceInfo(
        this.resource,
        this.resourceUrl,
        this.action
    )

    @JsonbTransient
    fun getMainResourceType() = channel.resourceType

    @JsonbTransient
    fun getMainResourceInfo() = ResourceInfo(
        getMainResourceType(),
        this.mainResourceUrl,
        if (getMainResourceType() == this.resource && this.mainResourceUrl == this.resourceUrl) {
            this.action
        } else {
            Action.UPDATE
        }
    )

    /**
     * Use this for the actually modified resource
     *
     * @param type   the type of resource
     * @param url    the identification of the resource
     * @param action the type of modification
     */
    data class ResourceInfo(val type: Resource, val url: URI, val action: Action)
}
