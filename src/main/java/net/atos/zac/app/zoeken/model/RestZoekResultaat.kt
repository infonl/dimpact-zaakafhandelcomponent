/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.zoeken.model.FilterResultaat
import net.atos.zac.zoeken.model.FilterVeld
import java.util.TreeMap

data class RestZoekResultaat<TYPE>(
    var results: Collection<TYPE>,
    var resultCount: Long,
    var filters: TreeMap<FilterVeld, MutableList<FilterResultaat>> =
        TreeMap<FilterVeld, MutableList<FilterResultaat>>(Comparator.comparingInt { it.ordinal })
) : RESTResultaat<TYPE>(results, resultCount)
