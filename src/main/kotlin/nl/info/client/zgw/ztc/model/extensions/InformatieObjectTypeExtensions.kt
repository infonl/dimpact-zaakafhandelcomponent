/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model.extensions

import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import java.time.LocalDate

fun InformatieObjectType.isNuGeldig(): Boolean =
    this.eindeGeldigheid.let { eindeGeldigheid ->
        this.beginGeldigheid.isBefore(
            LocalDate.now().plusDays(1)
        ) && (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()))
    }
