/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag.util

import nl.info.zac.productaanvraag.model.generated.Betrokkene

fun <T> Betrokkene.performAction(
    onNatuurlijkPersoonIdentity: (identity: String) -> T,
    onKvkIdentity: (kvkNummer: String, vestigingsNummer: String?) -> T,
    onNoIdentity: () -> T
) =
    when {
        kvkNummer != null -> onKvkIdentity(kvkNummer, vestigingsNummer)
        inpBsn != null -> onNatuurlijkPersoonIdentity(inpBsn)
        else -> onNoIdentity()
    }
