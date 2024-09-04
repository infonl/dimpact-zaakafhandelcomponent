package net.atos.client.zgw.ztc.model.extensions

import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import java.time.LocalDate

fun InformatieObjectType.isNuGeldig(): Boolean =
    this.eindeGeldigheid.let { eindeGeldigheid ->
        this.beginGeldigheid.isBefore(
            LocalDate.now().plusDays(1)
        ) && (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()))
    }
