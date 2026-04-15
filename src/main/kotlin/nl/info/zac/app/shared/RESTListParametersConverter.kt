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

abstract class RESTListParametersConverter<LP : ListParameters, RLP : RESTListParameters> {

    fun convert(restListParameters: RLP?): LP {
        val listParameters = getListParameters()
        if (restListParameters == null) {
            return listParameters
        }
        if (!restListParameters.sort.isNullOrBlank()) {
            listParameters.sorting = Sorting(restListParameters.sort, fromValue(restListParameters.order))
        }
        listParameters.paging = Paging(restListParameters.page, restListParameters.maxResults)
        doConvert(listParameters, restListParameters)
        return listParameters
    }

    protected abstract fun doConvert(listParameters: LP, restListParameters: RLP)

    protected abstract fun getListParameters(): LP
}
