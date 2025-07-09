package nl.info.zac.productaanvraag.util

import nl.info.zac.productaanvraag.model.generated.Betrokkene

fun <T> Betrokkene.performAction(
    onNatuurlijkPersoonIdentity: (identity: String) -> T,
    onVestigingIdentity: (identity: String) -> T,
    onNoIdentity: () -> T
) =
    when {
        inpBsn != null -> onNatuurlijkPersoonIdentity(inpBsn)
        vestigingsNummer != null -> onVestigingIdentity(vestigingsNummer)
        else -> onNoIdentity()
    }
