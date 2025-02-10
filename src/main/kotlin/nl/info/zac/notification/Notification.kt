/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.json.bind.annotation.JsonbTransient
import java.net.URI
import java.time.ZonedDateTime

/**
 * Notification as defined by the [ZGW Notificatie API](https://vng-realisatie.github.io/gemma-zaken/standaard/notificaties).
 */
class Notification {
    @set:JsonbProperty("kanaal")
    lateinit var channel: Channel

    @set:JsonbProperty("hoofdObject")
    lateinit var mainResourceUrl: URI

    @set:JsonbProperty("resource")
    lateinit var resource: Resource

    @set:JsonbProperty("resourceUrl")
    lateinit var resourceUrl: URI

    @set:JsonbProperty("actie")
    lateinit var action: Action

    @set:JsonbProperty("aanmaakdatum")
    lateinit var creationDateTime: ZonedDateTime

    @set:JsonbProperty("kenmerken")
    var properties = mutableMapOf<String, String>()

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

    override fun toString() = "$channel, $resource, $action, $creationDateTime"

    /**
     * Use this for the actually modified resource
     *
     * @param type   the type of resource
     * @param url    the identification of the resource
     * @param action the type of modification
     */
    data class ResourceInfo(val type: Resource, val url: URI, val action: Action)
}
