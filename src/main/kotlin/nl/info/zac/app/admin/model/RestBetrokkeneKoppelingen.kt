/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.admin.model.ZaaktypeBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestBetrokkeneKoppelingen(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaaktypeConfiguration? = null,

    @get:JsonbProperty("isBrpKoppelenEnabled")
    @set:JsonbProperty("isBrpKoppelenEnabled")
    var isBrpKoppelenEnabled: Boolean = false,

    @get:JsonbProperty("isKvkKoppelenEnabled")
    @set:JsonbProperty("isKvkKoppelenEnabled")
    var isKvkKoppelenEnabled: Boolean = false,
)

fun ZaaktypeBetrokkeneParameters.toRestBetrokkeneKoppelingen(): RestBetrokkeneKoppelingen =
    RestBetrokkeneKoppelingen().apply {
        id = this@toRestBetrokkeneKoppelingen.id
        this@toRestBetrokkeneKoppelingen.isBrpKoppelenEnabled?.let { isBrpKoppelenEnabled = it }
        this@toRestBetrokkeneKoppelingen.isKvkKoppelenEnabled?.let { isKvkKoppelenEnabled = it }
    }

fun RestBetrokkeneKoppelingen.toZaaktypeBetrokkenParameters(
    zaaktypeConfiguration: ZaaktypeConfiguration
): ZaaktypeBetrokkeneParameters = ZaaktypeBetrokkeneParameters().apply {
    id = this@toZaaktypeBetrokkenParameters.id
    isBrpKoppelenEnabled = this@toZaaktypeBetrokkenParameters.isBrpKoppelenEnabled
    isKvkKoppelenEnabled = this@toZaaktypeBetrokkenParameters.isKvkKoppelenEnabled
    this.zaaktypeConfiguration = zaaktypeConfiguration
}
