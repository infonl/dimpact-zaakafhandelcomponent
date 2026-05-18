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
import nl.info.zac.identification.IdentificationService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

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

    /** Only populated when type is [NATUURLIJK_PERSOON] */
    var bsn: String?,

    /**
     * Temporary UUID that can be used to look up the person instead of using sensitive BSN.
     * Only populated when type is [NATUURLIJK_PERSOON]
     */
    var temporaryPersonId: UUID?,

    /**
     * The identificatieType indicating what kind of bedrijf identifier is present.
     * Only set for [NIET_NATUURLIJK_PERSOON] and [VESTIGING].
     */
    var identificatieType: IdentificatieType?,

    /** Only populated when type is [VESTIGING] or [NIET_NATUURLIJK_PERSOON] with [IdentificatieType.VN] */
    var vestigingsnummer: String?,

    /** Only populated when type is [NIET_NATUURLIJK_PERSOON] with [IdentificatieType.RSIN] */
    var rsin: String?,

    /** Only populated when type is [NIET_NATUURLIJK_PERSOON] */
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
 * @param identificationService the [IdentificationService] instance to use for BSN replacement, or `null` to skip replacement
 * @returns the converted [RestZaakBetrokkene], or `null` if the rol has no [Rol.betrokkeneIdentificatie]
 * since we do not support [RestZaakBetrokkene] objects without an identification.
 */
@Suppress("ReturnCount", "CyclomaticComplexMethod")
fun Rol<*>.toRestZaakBetrokkene(identificationService: IdentificationService? = null): RestZaakBetrokkene? {
    var bsn: String? = null
    var temporaryPersonId: UUID? = null
    var identificatieType: IdentificatieType? = null
    var vestigingsnummer: String? = null
    var rsin: String? = null
    var kvkNummer: String? = null
    when (this.betrokkeneType) {
        NATUURLIJK_PERSOON -> {
            bsn = (this as RolNatuurlijkPersoon).betrokkeneIdentificatie?.inpBsn ?: return null
            identificatieType = IdentificatieType.BSN
            identificationService?.let { temporaryPersonId = identificationService.replaceBsnWithKey(bsn) }
        }
        NIET_NATUURLIJK_PERSOON -> {
            // A niet-natuurlijk persoon in the ZGW ZRC API can be either a KVK niet-natuurlijk persoon with an INN NNP ID (=RSIN)
            // or a KVK vestiging with a vestigingsnummer.
            // If the INN NNP ID is not present (and note that it may be an empty string), we use the vestigingsnummer.
            val betrokkene = (this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie ?: return null
            if (!betrokkene.innNnpId.isNullOrBlank()) {
                rsin = betrokkene.innNnpId
                identificatieType = IdentificatieType.RSIN
            } else if (!betrokkene.vestigingsNummer.isNullOrBlank()) {
                vestigingsnummer = betrokkene.vestigingsNummer
                identificatieType = IdentificatieType.VN
            } else {
                return null
            }
            kvkNummer = betrokkene.kvkNummer
        }
        VESTIGING -> {
            vestigingsnummer = (this as RolVestiging).betrokkeneIdentificatie?.vestigingsNummer ?: return null
            identificatieType = IdentificatieType.VN
        }
        ORGANISATORISCHE_EENHEID -> {
            (this as RolOrganisatorischeEenheid).betrokkeneIdentificatie?.naam ?: return null
        }
        MEDEWERKER -> {
            (this as RolMedewerker).betrokkeneIdentificatie?.identificatie ?: return null
        }
    }
    return RestZaakBetrokkene(
        rolid = this.uuid.toString(),
        roltype = this.omschrijving,
        roltoelichting = this.roltoelichting,
        type = this.betrokkeneType.name,
        bsn = bsn,
        temporaryPersonId = temporaryPersonId,
        identificatieType = identificatieType,
        vestigingsnummer = vestigingsnummer,
        rsin = rsin,
        kvkNummer = kvkNummer
    )
}

fun List<Rol<*>>.toRestZaakBetrokkenen(identificationService: IdentificationService? = null): List<RestZaakBetrokkene> =
    mapNotNull { it.toRestZaakBetrokkene(identificationService) }
