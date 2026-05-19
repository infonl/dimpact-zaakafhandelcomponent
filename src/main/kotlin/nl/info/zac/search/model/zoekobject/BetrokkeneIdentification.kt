/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon

enum class BetrokkeneIdentificationType(val prefix: String) {
    USER("U"),
    PERSON("P"),
    KVK_INSCHRIJVING("K"),
    KVK_VESTIGING("V")
}

data class BetrokkeneIdentification(
    val type: BetrokkeneIdentificationType,
    val identification: String
)

private fun buildPerson(bsn: String) =
    BetrokkeneIdentification(
        type = BetrokkeneIdentificationType.PERSON,
        identification = bsn,
    )

private fun buildKvkInschrijving(kvkNummer: String) =
    BetrokkeneIdentification(
        type = BetrokkeneIdentificationType.KVK_INSCHRIJVING,
        identification = kvkNummer,
    )

private fun buildKvkVestiging(kvkNummer: String, vestigingsnummer: String) =
    BetrokkeneIdentification(
        type = BetrokkeneIdentificationType.KVK_VESTIGING,
        identification = "$kvkNummer-$vestigingsnummer",
    )

private fun buildUser(username: String) =
    BetrokkeneIdentification(
        type = BetrokkeneIdentificationType.USER,
        identification = username,
    )

fun BetrokkeneIdentification.toSolrFormatting() =
    "${this.type.prefix}-${this.identification}"

fun Rol<*>.toBetrokkeneIdentification(): BetrokkeneIdentification? = when (this) {
    is RolNatuurlijkPersoon -> identificatienummer?.let {
        buildPerson(it)
    }
    is RolNietNatuurlijkPersoon -> toNietNatuurlijkPersoonIdentification()
    else -> identificatienummer?.let {
        buildUser(it)
    }
}

private fun RolNietNatuurlijkPersoon.toNietNatuurlijkPersoonIdentification(): BetrokkeneIdentification? {
    val nietNatuurlijkPersoonIdentificatie = betrokkeneIdentificatie ?: return null
    val vestigingsnummer = nietNatuurlijkPersoonIdentificatie.vestigingsNummer
    return nietNatuurlijkPersoonIdentificatie.kvkNummer?.let { kvkNummer ->
        when {
            vestigingsnummer != null &&
                vestigingsnummer.isNotBlank() -> buildKvkVestiging(kvkNummer, vestigingsnummer)
            else -> buildKvkInschrijving(kvkNummer)
        }
    }
}
