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
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.MEDEWERKER
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.NATUURLIJK_PERSOON
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.VESTIGING
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
        NATUURLIJK_PERSOON -> (this as RolNatuurlijkPersoon).betrokkeneIdentificatie?.inpBsn
        // A niet-natuurlijk persoon in the ZGW ZRC API can be either a KVK niet-natuurlijk persoon with an INN NNP ID (=RSIN)
        // _or_ a KVK vestiging with a vestigingsnummer.
        // If the INN NNP ID is not present (and note that it may be an empty string), we use the vestigingsnummer.
        NIET_NATUURLIJK_PERSOON -> (this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie?.let {
            it.innNnpId.takeIf { innNnpId -> innNnpId?.isNotBlank() == true } ?: it.vestigingsNummer
        }
        VESTIGING -> (this as RolVestiging).betrokkeneIdentificatie?.vestigingsNummer
        ORGANISATORISCHE_EENHEID -> (this as RolOrganisatorischeEenheid).betrokkeneIdentificatie?.naam
        MEDEWERKER -> (this as RolMedewerker).betrokkeneIdentificatie?.identificatie
    }
)

fun List<Rol<*>>.toRestZaakBetrokkenen(): List<RestZaakBetrokkene> = this.map { it.toRestZaakBetrokkene() }
