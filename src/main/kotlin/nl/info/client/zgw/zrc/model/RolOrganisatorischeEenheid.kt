/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.ztc.model.generated.RolType
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.util.Objects
import java.util.UUID

/**
 * Manually copied from [nl.info.client.zgw.zrc.model.generated.RolOrganisatorischeEenheid] and modified to allow for
 * polymorphism using a generic base [Rol] class.
 * Ideally we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 */
class RolOrganisatorischeEenheid : Rol<OrganisatorischeEenheidIdentificatie> {
    constructor() : super()

    /**
     * For testing purposes only where we need a UUID.
     */
    constructor(
        uuid: UUID,
        roltype: RolType,
        roltoelichting: String,
        betrokkeneIdentificatie: OrganisatorischeEenheidIdentificatie?
    ) : super(uuid, roltype, BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID, betrokkeneIdentificatie, roltoelichting)

    constructor(
        zaak: URI,
        roltype: RolType,
        roltoelichting: String,
        organisatorischeEenheid: OrganisatorischeEenheidIdentificatie?
    ) : super(zaak, roltype, BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID, organisatorischeEenheid, roltoelichting)

    @Suppress("ReturnCount")
    override fun equalBetrokkeneIdentificatie(identificatie: OrganisatorischeEenheidIdentificatie?): Boolean {
        val ownIdentificatie = betrokkeneIdentificatie
        if (ownIdentificatie === identificatie) {
            return true
        }
        if (identificatie == null) {
            return false
        }
        return Objects.equals(ownIdentificatie?.identificatie, identificatie.identificatie)
    }

    override val naam: String?
        get() {
            val identificatie = betrokkeneIdentificatie ?: return null
            return if (StringUtils.isNotEmpty(identificatie.naam)) identificatie.naam else identificatienummer
        }

    override val identificatienummer: String?
        get() = betrokkeneIdentificatie?.identificatie

    override fun hashCodeBetrokkeneIdentificatie(): Int = Objects.hash(betrokkeneIdentificatie?.identificatie)
}
