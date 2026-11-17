/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model.extensions

import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.time.ZacTime
import nl.info.zac.time.numberOfDaysFromToday
import java.time.Period

fun ZaakType.isServicenormAvailable(): Boolean = this.servicenorm != null

fun ZaakType.isNuGeldig(): Boolean =
    this.eindeGeldigheid.let { eindeGeldigheid ->
        this.beginGeldigheid.isBefore(ZacTime.today().plusDays(1)) &&
            (eindeGeldigheid == null || eindeGeldigheid.isAfter(ZacTime.today()))
    }

fun ZaakType.extensionPeriodDays(): Int? =
    if (this.verlengingMogelijk == true) {
        this.verlengingstermijn?.takeIf(String::isNotBlank)?.let { Period.parse(it).numberOfDaysFromToday() }
    } else {
        null
    }
