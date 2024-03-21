package net.atos.client.zgw.ztc.util

import net.atos.client.zgw.ztc.model.generated.ZaakType
import java.time.LocalDate
import java.time.Period

fun isServicenormBeschikbaar(zaakType: ZaakType): Boolean {
    return zaakType.servicenorm != null &&
        !Period.parse(zaakType.servicenorm).normalized().isZero
}

fun isNuGeldig(zaakType: ZaakType): Boolean {
    val eindeGeldigheid = zaakType.eindeGeldigheid
    return zaakType.beginGeldigheid.isBefore(
        LocalDate.now().plusDays(1)
    ) && (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()))
}
