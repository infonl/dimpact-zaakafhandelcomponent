/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestZaakBetrokkene(
    var rolid: String,

    var roltype: String,

    var roltoelichting: String?,

    var type: BetrokkeneType,

    var identificatie: String
)

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
fun Rol<*>.toRestZaakBetrokkene() = RestZaakBetrokkene(
    rolid = this.uuid.toString(),
    roltype = this.omschrijving,
    roltoelichting = this.roltoelichting,
    type = this.betrokkeneType,
    identificatie = when (this.betrokkeneType) {
        BetrokkeneType.NATUURLIJK_PERSOON -> (this as RolNatuurlijkPersoon).betrokkeneIdentificatie.inpBsn
        BetrokkeneType.NIET_NATUURLIJK_PERSOON -> (this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie.innNnpId
        BetrokkeneType.VESTIGING -> (this as RolVestiging).betrokkeneIdentificatie.vestigingsNummer
        BetrokkeneType.ORGANISATORISCHE_EENHEID -> (this as RolOrganisatorischeEenheid).betrokkeneIdentificatie.naam
        BetrokkeneType.MEDEWERKER -> (this as RolMedewerker).betrokkeneIdentificatie.identificatie
    }
)

fun List<Rol<*>>.toRestZaakBetrokkenen(): List<RestZaakBetrokkene> = this.map { it.toRestZaakBetrokkene() }
