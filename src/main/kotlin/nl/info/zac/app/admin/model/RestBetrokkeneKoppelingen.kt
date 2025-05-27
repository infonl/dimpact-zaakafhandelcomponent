/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import net.atos.zac.admin.model.BetrokkeneKoppelingen
import net.atos.zac.admin.model.ZaakafhandelParameters
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestBetrokkeneKoppelingen(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaakafhandelParameters? = null,
    var brpKoppelen: Boolean = false,
    var kvkKoppelen: Boolean = false,
)

fun BetrokkeneKoppelingen.toRestBetrokkeneKoppelingen(): RestBetrokkeneKoppelingen =
    RestBetrokkeneKoppelingen().apply {
        id = this@toRestBetrokkeneKoppelingen.id
        brpKoppelen = this@toRestBetrokkeneKoppelingen.brpKoppelen
        kvkKoppelen = this@toRestBetrokkeneKoppelingen.kvkKoppelen
    }

fun RestBetrokkeneKoppelingen.toBetrokkeneKoppelingen(
    zaakafhandelParameters: ZaakafhandelParameters
): BetrokkeneKoppelingen = BetrokkeneKoppelingen().apply {
    id = this@toBetrokkeneKoppelingen.id
    brpKoppelen = this@toBetrokkeneKoppelingen.brpKoppelen
    kvkKoppelen = this@toBetrokkeneKoppelingen.kvkKoppelen
    this.zaakafhandelParameters = zaakafhandelParameters
}
