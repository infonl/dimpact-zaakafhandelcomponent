/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model.extensions

import nl.info.client.zgw.ztc.model.generated.ZaakType
import java.time.LocalDate
import java.time.Period

fun ZaakType.isServicenormBeschikbaar(): Boolean =
    this.servicenorm != null && !Period.parse(this.servicenorm).normalized().isZero

fun ZaakType.isNuGeldig(): Boolean =
    this.eindeGeldigheid.let { eindeGeldigheid ->
        this.beginGeldigheid.isBefore(LocalDate.now().plusDays(1)) &&
            (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()))
    }
