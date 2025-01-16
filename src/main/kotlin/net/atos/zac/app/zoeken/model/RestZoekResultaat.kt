/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.zoeken.model.FilterResultaat
import net.atos.zac.zoeken.model.FilterVeld
import java.util.TreeMap

data class RestZoekResultaat<TYPE>(
    @get:JsonbProperty("resultaten")
    var results: Collection<TYPE>,

    @get:JsonbProperty("totaal")
    var resultCount: Long,

    var filters: TreeMap<FilterVeld, MutableList<FilterResultaat>> =
        TreeMap<FilterVeld, MutableList<FilterResultaat>>(Comparator.comparingInt { it.ordinal })
) : RESTResultaat<TYPE>(results, resultCount)
