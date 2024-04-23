/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RESTTaakVrijgevenGegevens(
    var taken: List<RESTTaakVerdelenTaak>,

    var reden: String? = null,

    /**
     * Unique screen event resource ID which can be used
     * to track the progress of the 'assign taken from list' asynchronous job
     * using web sockets.
     */
    var screenEventResourceId: String? = null
)
