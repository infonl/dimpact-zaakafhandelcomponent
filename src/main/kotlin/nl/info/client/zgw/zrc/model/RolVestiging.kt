/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.VestigingIdentificatie
import nl.info.client.zgw.ztc.model.generated.RolType
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.util.Objects

/**
 * Manually copied from [nl.info.client.zgw.zrc.model.generated.RolVestiging] and modified to allow for
 * polymorphism using a generic base [Rol] class.
 * Ideally we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 *
 * In ZAC, we only use the vestiging role for retrieving existing roles, but not / no longer for storing new roles.
 * For storing new vestiging-type roles, please use [nl.info.client.zgw.zrc.model.generated.RolNietNatuurlijkPersoon] instead for
 * vestigingen.
 * For details see: [Add 'vestigingsnummer' to NNP, deprecate 'vestiging'](https://github.com/open-zaak/open-zaak/issues/1935).
 */
class RolVestiging : Rol<VestigingIdentificatie> {
    constructor() : super()

    constructor(
        zaak: URI,
        roltype: RolType,
        roltoelichting: String,
        betrokkeneIdentificatie: VestigingIdentificatie?
    ) : super(zaak, roltype, BetrokkeneTypeEnum.VESTIGING, betrokkeneIdentificatie, roltoelichting)

    @Suppress("ReturnCount")
    override fun equalBetrokkeneIdentificatie(identificatie: VestigingIdentificatie?): Boolean {
        val ownIdentificatie = betrokkeneIdentificatie
        if (ownIdentificatie === identificatie) {
            return true
        }
        if (identificatie == null) {
            return false
        }
        return Objects.equals(ownIdentificatie?.vestigingsNummer, identificatie.vestigingsNummer)
    }

    override val naam: String?
        get() {
            val identificatie = betrokkeneIdentificatie ?: return null
            val namen = identificatie.handelsnaam?.joinToString("; ")
            return if (StringUtils.isNotEmpty(namen)) namen else identificatienummer
        }

    override val identificatienummer: String?
        get() = betrokkeneIdentificatie?.vestigingsNummer

    override fun hashCodeBetrokkeneIdentificatie(): Int = Objects.hash(betrokkeneIdentificatie?.vestigingsNummer)
}
