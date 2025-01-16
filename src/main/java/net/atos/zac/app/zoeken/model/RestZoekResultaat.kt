/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.zoeken.model.FilterResultaat
import net.atos.zac.zoeken.model.FilterVeld
import java.util.TreeMap
import java.util.function.ToIntFunction

class RestZoekResultaat<TYPE> : RESTResultaat<TYPE> {
    var filters: TreeMap<FilterVeld, MutableList<FilterResultaat>> =
        TreeMap<FilterVeld, MutableList<FilterResultaat>>(
            Comparator.comparingInt<FilterVeld>(ToIntFunction { it.ordinal })
        )

    constructor(resultaten: MutableCollection<TYPE>, aantalTotaal: Long) : super(resultaten, aantalTotaal)

    constructor(foutmelding: String?) : super(foutmelding)
}
