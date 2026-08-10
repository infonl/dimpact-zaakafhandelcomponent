/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.ztc.model.generated.RolType
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.util.Objects
import java.util.UUID

/**
 * Manually copied from [nl.info.client.zgw.zrc.model.generated.RolNietNatuurlijkPersoon] and modified to allow for
 * polymorphism using a generic base [Rol] class.
 * Ideally, we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 */
class RolNietNatuurlijkPersoon : Rol<NietNatuurlijkPersoonIdentificatie> {
    constructor() : super()

    constructor(
        zaak: URI,
        roltype: RolType,
        roltoelichting: String,
        betrokkeneIdentificatie: NietNatuurlijkPersoonIdentificatie?
    ) : super(zaak, roltype, BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting)

    /**
     * For testing purposes only where we need a UUID.
     */
    constructor(
        uuid: UUID,
        roltype: RolType,
        roltoelichting: String,
        betrokkeneIdentificatie: NietNatuurlijkPersoonIdentificatie?
    ) : super(uuid, roltype, BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON, betrokkeneIdentificatie, roltoelichting)

    @Suppress("ReturnCount")
    override fun equalBetrokkeneIdentificatie(identificatie: NietNatuurlijkPersoonIdentificatie?): Boolean {
        val ownIdentificatie = betrokkeneIdentificatie
        if (ownIdentificatie === identificatie) {
            return true
        }
        if (identificatie == null || ownIdentificatie == null) {
            return false
        }
        if (ownIdentificatie.innNnpId != null || identificatie.innNnpId != null) {
            return Objects.equals(ownIdentificatie.innNnpId, identificatie.innNnpId)
        }
        if (ownIdentificatie.kvkNummer != null || identificatie.kvkNummer != null) {
            return Objects.equals(ownIdentificatie.kvkNummer, identificatie.kvkNummer)
        }
        if (ownIdentificatie.vestigingsNummer != null || identificatie.vestigingsNummer != null) {
            return Objects.equals(ownIdentificatie.vestigingsNummer, identificatie.vestigingsNummer)
        }
        if (ownIdentificatie.annIdentificatie != null || identificatie.annIdentificatie != null) {
            return Objects.equals(ownIdentificatie.annIdentificatie, identificatie.annIdentificatie)
        }
        return true
    }

    override val naam: String?
        get() {
            val identificatie = betrokkeneIdentificatie ?: return null
            return if (StringUtils.isNotEmpty(identificatie.statutaireNaam)) identificatie.statutaireNaam else identificatienummer
        }

    override val identificatienummer: String?
        get() {
            val identificatie = betrokkeneIdentificatie ?: return null
            return when {
                // new 'RSIN-type' initiators only have a KVK number (but no vestigingsnummer)
                StringUtils.isNotBlank(identificatie.kvkNummer) && StringUtils.isBlank(identificatie.vestigingsNummer) ->
                    identificatie.kvkNummer
                // we also support 'legacy' RSIN-type initiators with only an RSIN (no KVK number)
                StringUtils.isNotEmpty(identificatie.innNnpId) -> identificatie.innNnpId
                // lastly we support the 'vestiging-type' initiators that have both a KVK number and a vestigingsnummer
                // note that the KVK number is not part of this class but returned differently
                else -> identificatie.vestigingsNummer
            }
        }

    override fun hashCodeBetrokkeneIdentificatie(): Int {
        val identificatie = betrokkeneIdentificatie ?: return 0
        return when {
            identificatie.innNnpId != null -> Objects.hash(identificatie.innNnpId)
            identificatie.kvkNummer != null -> Objects.hash(identificatie.kvkNummer)
            identificatie.vestigingsNummer != null -> Objects.hash(identificatie.vestigingsNummer)
            identificatie.annIdentificatie != null -> Objects.hash(identificatie.annIdentificatie)
            else -> 0
        }
    }
}
