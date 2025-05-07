/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter

import net.atos.zac.admin.model.BetrokkeneKoppelingen
import net.atos.zac.admin.model.ZaakafhandelParameters
import nl.info.zac.app.admin.model.RestBetrokkeneKoppelingen

object RestBetrokkeneKoppelingenConverter {
    fun fromBetrokkeneKoppelingen(betrokkeneKoppelingen: BetrokkeneKoppelingen): RestBetrokkeneKoppelingen {
        return RestBetrokkeneKoppelingen().apply {
            id = betrokkeneKoppelingen.id
            brpKoppelen = betrokkeneKoppelingen.brpKoppelen
            kvkKoppelen = betrokkeneKoppelingen.kvkKoppelen
        }
    }

    fun fromRestBetrokkeneKoppelingen(
        restBetrokkeneKoppelingen: RestBetrokkeneKoppelingen,
        zaakafhandelParameters: ZaakafhandelParameters
    ): BetrokkeneKoppelingen {
        return BetrokkeneKoppelingen().apply {
            id = restBetrokkeneKoppelingen.id
            brpKoppelen = restBetrokkeneKoppelingen.brpKoppelen
            kvkKoppelen = restBetrokkeneKoppelingen.kvkKoppelen
            this.zaakafhandelParameters = zaakafhandelParameters
        }
    }
}
