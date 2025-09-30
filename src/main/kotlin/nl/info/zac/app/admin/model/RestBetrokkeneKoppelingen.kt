/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeCmmnBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestBetrokkeneKoppelingen(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaakafhandelParameters? = null,
    var brpKoppelen: Boolean = false,
    var kvkKoppelen: Boolean = false,
)

fun ZaaktypeCmmnBetrokkeneParameters.toRestBetrokkeneKoppelingen(): RestBetrokkeneKoppelingen =
    RestBetrokkeneKoppelingen().apply {
        id = this@toRestBetrokkeneKoppelingen.id
        brpKoppelen = this@toRestBetrokkeneKoppelingen.brpKoppelen!!
        kvkKoppelen = this@toRestBetrokkeneKoppelingen.kvkKoppelen!!
    }

fun RestBetrokkeneKoppelingen.toBetrokkeneKoppelingen(
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
): ZaaktypeCmmnBetrokkeneParameters = ZaaktypeCmmnBetrokkeneParameters().apply {
    id = this@toBetrokkeneKoppelingen.id
    brpKoppelen = this@toBetrokkeneKoppelingen.brpKoppelen
    kvkKoppelen = this@toBetrokkeneKoppelingen.kvkKoppelen
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
}
