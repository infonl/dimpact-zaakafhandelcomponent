/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.shared

import net.atos.zac.app.shared.RESTListParameters
import nl.info.zac.shared.model.ListParameters
import nl.info.zac.shared.model.Paging
import nl.info.zac.shared.model.Sorting
import nl.info.zac.shared.model.fromValue

fun <LP : ListParameters> RESTListParameters.applyCommonParametersTo(listParameters: LP): LP {
    if (!sort.isNullOrBlank()) listParameters.sorting = Sorting(sort, fromValue(order))
    listParameters.paging = Paging(page, maxResults)
    return listParameters
}
