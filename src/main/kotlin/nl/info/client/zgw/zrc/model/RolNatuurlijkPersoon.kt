/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.ztc.model.generated.RolType
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.util.Objects
import java.util.UUID

/**
 * Manually copied from [nl.info.client.zgw.zrc.model.generated.RolNatuurlijkPersoon] and modified to allow for
 * polymorphism using a generic base [Rol] class.
 * Ideally, we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 */
class RolNatuurlijkPersoon : Rol<NatuurlijkPersoonIdentificatie> {
    constructor() : super()

    constructor(
        zaak: URI,
        roltype: RolType,
        roltoelichting: String,
        betrokkeneIdentificatie: NatuurlijkPersoonIdentificatie?
    ) : super(zaak, roltype, BetrokkeneTypeEnum.NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting)

    /**
     * For testing purposes only where we need a UUID.
     */
    constructor(
        uuid: UUID,
        roltype: RolType,
        roltoelichting: String,
        betrokkeneIdentificatie: NatuurlijkPersoonIdentificatie?
    ) : super(uuid, roltype, BetrokkeneTypeEnum.NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting)

    @Suppress("ReturnCount")
    override fun equalBetrokkeneIdentificatie(identificatie: NatuurlijkPersoonIdentificatie?): Boolean {
        val ownIdentificatie = betrokkeneIdentificatie
        if (ownIdentificatie === identificatie) {
            return true
        }
        if (identificatie == null) {
            return false
        }
        if (ownIdentificatie?.anpIdentificatie != null || identificatie.anpIdentificatie != null) {
            return Objects.equals(ownIdentificatie?.anpIdentificatie, identificatie.anpIdentificatie)
        }
        if (ownIdentificatie?.inpANummer != null || identificatie.inpANummer != null) {
            return Objects.equals(ownIdentificatie?.inpANummer, identificatie.inpANummer)
        }
        if (ownIdentificatie?.inpBsn != null || identificatie.inpBsn != null) {
            return Objects.equals(ownIdentificatie?.inpBsn, identificatie.inpBsn)
        }
        return true
    }

    override val naam: String?
        get() = betrokkeneIdentificatie?.let {
            if (StringUtils.isNotEmpty(it.voorvoegselGeslachtsnaam)) it.voorvoegselGeslachtsnaam else identificatienummer
        }

    override val identificatienummer: String?
        get() = betrokkeneIdentificatie?.inpBsn

    override fun hashCodeBetrokkeneIdentificatie(): Int {
        val identificatie = betrokkeneIdentificatie ?: return 0
        return when {
            identificatie.anpIdentificatie != null -> Objects.hash(identificatie.anpIdentificatie)
            identificatie.inpANummer != null -> Objects.hash(identificatie.inpANummer)
            identificatie.inpBsn != null -> Objects.hash(identificatie.inpBsn)
            else -> 0
        }
    }
}
