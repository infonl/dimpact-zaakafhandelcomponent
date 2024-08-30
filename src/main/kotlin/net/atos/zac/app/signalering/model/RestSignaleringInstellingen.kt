/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signalering.model

import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestSignaleringInstellingen(
    var id: Long? = null,

    var type: SignaleringType.Type,

    var subjecttype: SignaleringSubject? = null,

    var dashboard: Boolean? = null,

    var mail: Boolean? = null,
)
