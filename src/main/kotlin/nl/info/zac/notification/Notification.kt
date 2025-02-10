/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.json.bind.annotation.JsonbTransient
import java.net.URI
import java.time.ZonedDateTime

class Notification {
    @set:JsonbProperty("kanaal")
    var channel: Channel? = null

    @set:JsonbProperty("hoofdObject")
    var mainResourceUrl: URI? = null

    @set:JsonbProperty("resource")
    var resource: Resource? = null

    @set:JsonbProperty("resourceUrl")
    var resourceUrl: URI? = null

    @set:JsonbProperty("actie")
    var action: Action? = null

    @set:JsonbProperty("aanmaakdatum")
    var creationDateTime: ZonedDateTime? = null

    @set:JsonbProperty("kenmerken")
    var properties = mutableMapOf<String, String>()

    @JsonbTransient
    fun getResourceInfo() = ResourceInfo(
        this.resource,
        this.resourceUrl,
        this.action
    )

    @JsonbTransient
    fun getMainResourceType() = channel?.resourceType

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

    override fun toString(): String {
        return "$channel, $resource, $action, $creationDateTime"
    }

    /**
     * Use this for the actually modified resource
     *
     * @param type   the type of resource
     * @param url    the identification of the resource
     * @param action the type of modification
     */
    data class ResourceInfo(val type: Resource?, val url: URI?, val action: Action?)
}
