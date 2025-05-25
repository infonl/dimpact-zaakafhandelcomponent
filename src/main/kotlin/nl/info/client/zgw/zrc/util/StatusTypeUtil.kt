/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_HEROPEND
import nl.info.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_INTAKE

// TODO: make non-nullable
fun isHeropend(statusType: StatusType?): Boolean {
    return statusType != null && STATUSTYPE_OMSCHRIJVING_HEROPEND == statusType.getOmschrijving()
}

// TODO: make non-nullable
fun isIntake(statustype: StatusType?): Boolean {
    return statustype != null && STATUSTYPE_OMSCHRIJVING_INTAKE == statustype.getOmschrijving()
}
