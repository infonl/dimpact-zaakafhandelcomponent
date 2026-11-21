/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.app.shared.RESTResultaat
import nl.info.zac.search.model.FilterResultaat
import nl.info.zac.search.model.FilterVeld
import java.util.TreeMap

data class RestZoekResultaat<TYPE>(
    @get:JsonbProperty("resultaten")
    var results: Collection<TYPE>,

    @get:JsonbProperty("totaal")
    var resultCount: Long,

    var filters: TreeMap<FilterVeld, MutableList<FilterResultaat>> =
        TreeMap<FilterVeld, MutableList<FilterResultaat>>(Comparator.comparingInt { it.ordinal })
) : RESTResultaat<TYPE>(results, resultCount)
