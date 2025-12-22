/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model.extensions

import net.atos.zac.util.time.PeriodUtil
import nl.info.client.zgw.ztc.model.generated.ZaakType
import java.time.LocalDate
import java.time.Period

fun ZaakType.isServicenormAvailable(): Boolean =
    this.servicenorm != null && !Period.parse(this.servicenorm).normalized().isZero

fun ZaakType.isNuGeldig(): Boolean =
    this.eindeGeldigheid.let { eindeGeldigheid ->
        this.beginGeldigheid.isBefore(LocalDate.now().plusDays(1)) &&
            (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()))
    }

fun ZaakType.extensionPeriodDays(): Int? =
    if (this.verlengingMogelijk == true) {
        this.verlengingstermijn?.takeIf(String::isNotBlank)?.let { PeriodUtil.numberOfDaysFromToday(Period.parse(it)) }
    } else {
        null
    }
