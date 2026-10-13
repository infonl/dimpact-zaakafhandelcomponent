/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identification

import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.zac.app.klant.model.klant.IdentificatieType

/**
 * The 'niet_natuurlijk_persoon' role type is used both for rechtspersonen and for vestigingen.
 * We support 'legacy' rechtspersonen with only an RSIN, new rechtspersonen with only a KVK number,
 * and vestigingen with a vestigingsnummer.
 */
fun NietNatuurlijkPersoonIdentificatie.toIdentificatieType(): IdentificatieType? =
    when {
        innNnpId?.isNotBlank() == true ||
            (kvkNummer?.isNotBlank() == true && vestigingsNummer.isNullOrBlank()) -> IdentificatieType.RSIN
        vestigingsNummer?.isNotBlank() == true -> IdentificatieType.VN
        else -> null
    }

/**
 * Returns `null` when KVK finds nothing, and also when the identificatie has no supported identificatie type.
 */
fun KvkClientService.findRechtspersoonOrVestiging(
    nietNatuurlijkPersoonIdentificatie: NietNatuurlijkPersoonIdentificatie
): ResultaatItem? =
    when (nietNatuurlijkPersoonIdentificatie.toIdentificatieType()) {
        IdentificatieType.RSIN -> if (nietNatuurlijkPersoonIdentificatie.innNnpId?.isNotBlank() == true) {
            findRechtspersoonByRsin(nietNatuurlijkPersoonIdentificatie.innNnpId)
        } else {
            findRechtspersoonByKvkNummer(nietNatuurlijkPersoonIdentificatie.kvkNummer)
        }
        IdentificatieType.VN -> findVestiging(nietNatuurlijkPersoonIdentificatie.vestigingsNummer)
        else -> null
    }
