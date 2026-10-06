/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestSignaleringInstellingen(
    var id: Long? = null,

    var type: SignaleringType.Type,

    var subjecttype: SignaleringSubject? = null,

    @get:JsonbProperty("isDashboardEnabled")
    @set:JsonbProperty("isDashboardEnabled")
    var isDashboardEnabled: Boolean? = null,

    @get:JsonbProperty("isMailEnabled")
    @set:JsonbProperty("isMailEnabled")
    var isMailEnabled: Boolean? = null,
)
