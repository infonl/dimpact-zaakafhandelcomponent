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
     * - In case of a [NATUURLIJK_PERSOON] this is the BSN.
     * - In case of a [NIET_NATUURLIJK_PERSOON] this is the RSIN (innNnpId) if available, otherwise the `vestigingsnummer`.
     */
    var identificatie: String,

    /**
     * The identificatieType indicating what the type is of the [identificatie] field.
     * This is only set for certain betrokkene types, specifically for betrokkene types which support
     * multiple identificatie types like BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON.
     */
    var identificatieType: IdentificatieType?,

    /**
     * Only populated when type is [NIET_NATUURLIJK_PERSOON] and it is not a `INN NNP ID (=RSIN)`
     */
    var kvkNummer: String?
)

/**
 * Converts a [Rol] to a [RestZaakBetrokkene].
 *
 * Note that it is technically possible, according to the ZGW ZRC API, that a Rol does _not_ have a [Rol.betrokkeneIdentificatie].
 * This does happen in practice under certain circumstances when an 'empty' rol object is linked
 * to the zaak but when the rol object itself does not contain a betrokkene.
 * It can also happen that a specific betrokkene type does not have an actual identification field.
 * We do not return these roles as they do not contain enough useful information for the client.
 *
 * @returns the converted [RestZaakBetrokkene], or `null` if the rol has no [Rol.betrokkeneIdentificatie]
 * since we do not support [RestZaakBetrokkene] objects without an identification.
 */
@Suppress("ReturnCount")
fun Rol<*>.toRestZaakBetrokkene(): RestZaakBetrokkene? {
    var identificatieType: IdentificatieType? = null
    var identificatie: String
    var kvkNummer: String? = null
    when (this.betrokkeneType) {
        NATUURLIJK_PERSOON -> {
            identificatie = (this as RolNatuurlijkPersoon).betrokkeneIdentificatie?.inpBsn ?: return null
            identificatieType = IdentificatieType.BSN
        }
        NIET_NATUURLIJK_PERSOON -> {
            // A niet-natuurlijk persoon in the ZGW ZRC API can be either a KVK niet-natuurlijk persoon with an INN NNP ID (=RSIN)
            // or a KVK vestiging with a vestigingsnummer.
            // If the INN NNP ID is not present (and note that it may be an empty string), we use the vestigingsnummer.
            val betrokkene = (this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie ?: return null
            identificatie = betrokkene.innNnpId.takeIf { !it.isNullOrBlank() } ?: betrokkene.vestigingsNummer ?: return null
            identificatieType = if (betrokkene.innNnpId.isNullOrBlank()) IdentificatieType.VN else IdentificatieType.RSIN
            kvkNummer = betrokkene.kvkNummer ?: return null
        }
        VESTIGING -> {
            identificatie = (this as RolVestiging).betrokkeneIdentificatie?.vestigingsNummer ?: return null
            identificatieType = IdentificatieType.VN
        }
        ORGANISATORISCHE_EENHEID -> {
            identificatie = (this as RolOrganisatorischeEenheid).betrokkeneIdentificatie?.naam ?: return null
            // this betrokkene type has no identificatieType
            identificatieType = null
        }
        MEDEWERKER -> {
            identificatie = (this as RolMedewerker).betrokkeneIdentificatie?.identificatie ?: return null
            // this betrokkene type has no identificatieType
            identificatieType = null
        }
    }
    return RestZaakBetrokkene(
        rolid = this.uuid.toString(),
        roltype = this.omschrijving,
        roltoelichting = this.roltoelichting,
        type = this.betrokkeneType.name,
        identificatie = identificatie,
        identificatieType = identificatieType,
        kvkNummer = kvkNummer
    )
}

fun List<Rol<*>>.toRestZaakBetrokkenen(): List<RestZaakBetrokkene> = mapNotNull { it.toRestZaakBetrokkene() }
