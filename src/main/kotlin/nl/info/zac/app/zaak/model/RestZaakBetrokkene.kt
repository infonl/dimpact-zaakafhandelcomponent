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
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestZaakBetrokkene(
    var rolid: String,
    var roltype: String,
    var roltoelichting: String?,

    /**
     * The type of the betrokkene, as the name of [nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum].
     * We should just use an enum itself here (maybe our own enum), instead of a String.
     */
    var type: String,

    /**
     * The unique identifier of the betrokkene.
     */
    var identificatie: String,

    /**
     * The identificatieType indicating what the type is of the [identificatie] field.
     * This is only set for certain betrokkene types, specifically for betrokkene types which support
     * multiple identificatie types like BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON.
     */
    var identificatieType: IdentificatieType?
)

/**
 * Converts a [Rol] to a [RestZaakBetrokkene].
 *
 * Note that it is technically possible, according to the ZGW ZRC API, that a Rol does _not_ have a [Rol.betrokkeneIdentificatie].
 * This does happen in practice under certain circumstances when an 'empty' rol object is linked
 * to the zaak but when the rol object itself does not contain a betrokkene.
 * We do not return these roles as they do not contain enough useful information for the client.
 *
 * @returns the converted [RestZaakBetrokkene], or `null` if the rol has no [Rol.betrokkeneIdentificatie]
 * since we do not support [RestZaakBetrokkene] objects without an identification.
 */
fun Rol<*>.toRestZaakBetrokkene(): RestZaakBetrokkene? {
    if (this.betrokkeneIdentificatie == null) return null
    var identificatieType: IdentificatieType? = null
    var identificatie: String
    when (this.betrokkeneType) {
        NATUURLIJK_PERSOON -> {
            (this as RolNatuurlijkPersoon).betrokkeneIdentificatie.let {
                identificatie = it.inpBsn
                identificatieType = IdentificatieType.BSN
            }
        }
        // A niet-natuurlijk persoon in the ZGW ZRC API can be either a KVK niet-natuurlijk persoon with an INN NNP ID (=RSIN)
        // _or_ a KVK vestiging with a vestigingsnummer.
        // If the INN NNP ID is not present (and note that it may be an empty string), we use the vestigingsnummer.
        NIET_NATUURLIJK_PERSOON -> (this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie.let {
            if (it.innNnpId.isNullOrBlank()) {
                identificatie = it.vestigingsNummer
                identificatieType = IdentificatieType.VN
            } else {
                identificatie = it.innNnpId
                identificatieType = IdentificatieType.RSIN
            }
        }
        VESTIGING -> {
            identificatie = (this as RolVestiging).betrokkeneIdentificatie.vestigingsNummer
            identificatieType = IdentificatieType.VN
        }
        ORGANISATORISCHE_EENHEID -> {
            identificatie = (this as RolOrganisatorischeEenheid).betrokkeneIdentificatie.naam
            // the identificatie field of an organisatorische eenheid has no identificatieType; it is just a string
        }
        MEDEWERKER -> {
            identificatie = (this as RolMedewerker).betrokkeneIdentificatie.identificatie
            // the identificatie field of a medewerker has no identificatieType; it is just a string
        }
    }
    return RestZaakBetrokkene(
        rolid = this.uuid.toString(),
        roltype = this.omschrijving,
        roltoelichting = this.roltoelichting,
        type = this.betrokkeneType.name,
        identificatie = identificatie,
        identificatieType = identificatieType
    )
}

fun List<Rol<*>>.toRestZaakBetrokkenen(): List<RestZaakBetrokkene> = mapNotNull { it.toRestZaakBetrokkene() }
