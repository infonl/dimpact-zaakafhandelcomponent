/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.annotation.Nullable
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.ztc.model.generated.RolType
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.util.Objects
import java.util.UUID

/**
 * Manually copied from [nl.info.client.zgw.zrc.model.generated.RolMedewerker] and modified to allow for
 * polymorphism using a generic base [Rol] class.
 * Ideally, we would use the generated class, but currently we cannot get the OpenAPI Generator framework to generate
 * polymorphic relationships correctly.
 */
class RolMedewerker : Rol<MedewerkerIdentificatie> {
    constructor() : super()

    /**
     * For testing purposes only where we need a UUID.
     */
    constructor(
        uuid: UUID,
        roltype: RolType,
        roltoelichting: String,
        betrokkeneIdentificatie: MedewerkerIdentificatie?
    ) : super(uuid, roltype, BetrokkeneTypeEnum.MEDEWERKER, betrokkeneIdentificatie, roltoelichting)

    constructor(
        zaak: URI,
        roltype: RolType,
        roltoelichting: String,
        // it is possible in the ZGW API to have a RolMedewerker without a Medewerker,
        // and this does occur in practice in certain circumstances
        @Nullable medewerkerIdentificatie: MedewerkerIdentificatie?
    ) : super(zaak, roltype, BetrokkeneTypeEnum.MEDEWERKER, medewerkerIdentificatie, roltoelichting)

    override val naam: String?
        get() {
            val medewerker = betrokkeneIdentificatie ?: return null
            return if (StringUtils.isNotBlank(medewerker.achternaam)) {
                buildString {
                    if (StringUtils.isNotBlank(medewerker.voorletters)) {
                        append(medewerker.voorletters).append(StringUtils.SPACE)
                    }
                    if (StringUtils.isNotBlank(medewerker.voorvoegselAchternaam)) {
                        append(medewerker.voorvoegselAchternaam).append(StringUtils.SPACE)
                    }
                    append(medewerker.achternaam)
                }
            } else {
                medewerker.identificatie
            }
        }

    @Suppress("ReturnCount")
    override fun equalBetrokkeneIdentificatie(identificatie: MedewerkerIdentificatie?): Boolean {
        val ownIdentificatie = betrokkeneIdentificatie
        if (ownIdentificatie === identificatie) {
            return true
        }
        if (identificatie == null) {
            return false
        }
        return Objects.equals(ownIdentificatie?.identificatie, identificatie.identificatie)
    }

    override val identificatienummer: String?
        get() = betrokkeneIdentificatie?.identificatie

    override fun hashCodeBetrokkeneIdentificatie(): Int {
        val identificatie = betrokkeneIdentificatie ?: return -1
        return Objects.hash(identificatie.identificatie)
    }
}
