/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.shared.model.AbstractListParameters
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import java.net.URI

class RolListParameters : AbstractListParameters {
    /**
     * URL-referentie naar de ZAAK.
     */
    @QueryParam("zaak")
    var zaak: URI? = null

    /**
     * Type van de betrokkene
     */
    @QueryParam("betrokkeneType")
    var betrokkeneType: String? = null
        private set

    /**
     * URL-referentie naar een roltype binnen het ZAAKTYPE van de ZAAK.
     */
    @QueryParam("roltype")
    var roltype: URI? = null

    constructor(zaak: URI) : super() {
        this.zaak = zaak
    }

    constructor(zaak: URI, roltype: URI) : super() {
        this.zaak = zaak
        this.roltype = roltype
    }

    constructor(zaak: URI, roltype: URI, betrokkeneType: BetrokkeneTypeEnum) : super() {
        this.zaak = zaak
        this.betrokkeneType = betrokkeneType.toString()
        this.roltype = roltype
    }

    fun setBetrokkeneType(betrokkeneType: BetrokkeneTypeEnum) {
        this.betrokkeneType = betrokkeneType.toString()
    }
}
