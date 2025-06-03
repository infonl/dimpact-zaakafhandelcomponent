/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestZaakBetrokkene(
    var rolid: String,

    var roltype: String,

    var roltoelichting: String?,

    var type: String,

    var identificatie: String?
)

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
fun Rol<*>.toRestZaakBetrokkene() = RestZaakBetrokkene(
    rolid = this.uuid.toString(),
    roltype = this.omschrijving,
    roltoelichting = this.roltoelichting,
    type = this.betrokkeneType.name,
    identificatie = when (this.betrokkeneType) {
        BetrokkeneTypeEnum.NATUURLIJK_PERSOON -> (this as RolNatuurlijkPersoon).betrokkeneIdentificatie?.inpBsn
        BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON -> (this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie?.innNnpId
        BetrokkeneTypeEnum.VESTIGING -> (this as RolVestiging).betrokkeneIdentificatie?.vestigingsNummer
        BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID -> (this as RolOrganisatorischeEenheid).betrokkeneIdentificatie?.naam
        BetrokkeneTypeEnum.MEDEWERKER -> (this as RolMedewerker).betrokkeneIdentificatie?.identificatie
    }
)

fun List<Rol<*>>.toRestZaakBetrokkenen(): List<RestZaakBetrokkene> = this.map { it.toRestZaakBetrokkene() }
