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
    KVK("K"),
    KVK_VESTIGING("V")
}

data class BetrokkeneIdentification(
    val type: BetrokkeneIdentificationType,
    val identification: String
) {
    companion object {
        fun buildPerson(bsn: String) =
            BetrokkeneIdentification(
                type = BetrokkeneIdentificationType.PERSON,
                identification = bsn,
            )
        fun buildKvk(kvkNummer: String) =
            BetrokkeneIdentification(
                type = BetrokkeneIdentificationType.KVK,
                identification = kvkNummer,
            )
        fun buildKvkVestiging(kvkNummer: String, vestigingsnummer: String) =
            BetrokkeneIdentification(
                type = BetrokkeneIdentificationType.KVK_VESTIGING,
                identification = "$kvkNummer-$vestigingsnummer",
            )
        fun buildUser(username: String) =
            BetrokkeneIdentification(
                type = BetrokkeneIdentificationType.USER,
                identification = username,
            )
    }
}

fun BetrokkeneIdentification.toSolr() =
    "${this.type.prefix}-${this.identification}"

fun Rol<*>.toBetrokkeneIdentification(): BetrokkeneIdentification? = when (this) {
    is RolNatuurlijkPersoon -> identificatienummer?.let {
        BetrokkeneIdentification.buildPerson(it)
    }
    is RolNietNatuurlijkPersoon -> toNietNatuurlijkPersoonIdentification()
    else -> identificatienummer?.let {
        BetrokkeneIdentification.buildUser(it)
    }
}

private fun RolNietNatuurlijkPersoon.toNietNatuurlijkPersoonIdentification(): BetrokkeneIdentification? {
    val nietNatuurlijkPersoonIdentificatie = betrokkeneIdentificatie ?: return null
    val vestigingsnummer = nietNatuurlijkPersoonIdentificatie.vestigingsNummer
    return nietNatuurlijkPersoonIdentificatie.kvkNummer?.let { kvkNummer ->
        when {
            vestigingsnummer != null &&
                vestigingsnummer.isNotBlank() -> BetrokkeneIdentification.buildKvkVestiging(kvkNummer, vestigingsnummer)
            else -> BetrokkeneIdentification.buildKvk(kvkNummer)
        }
    }
}
