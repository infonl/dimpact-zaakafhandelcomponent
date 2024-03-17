package net.atos.client.zgw.ztc.util

import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import java.time.LocalDate

object InformatieObjectTypeUtil {
    @JvmStatic
    fun isNuGeldig(informatieObjectType: InformatieObjectType): Boolean {
        val eindeGeldigheid = informatieObjectType.eindeGeldigheid
        return informatieObjectType.beginGeldigheid.isBefore(
            LocalDate.now().plusDays(1)
        ) && (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()))
    }
}
