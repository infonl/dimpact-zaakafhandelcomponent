/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model.extensions

import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.zac.time.ZacTime

fun InformatieObjectType.isNuGeldig(): Boolean =
    this.eindeGeldigheid.let { eindeGeldigheid ->
        this.beginGeldigheid.isBefore(
            ZacTime.today().plusDays(1)
        ) && (eindeGeldigheid == null || eindeGeldigheid.isAfter(ZacTime.today()))
    }
