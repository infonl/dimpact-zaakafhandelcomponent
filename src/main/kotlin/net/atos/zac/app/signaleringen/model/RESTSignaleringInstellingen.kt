/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signaleringen.model

import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringType

class RESTSignaleringInstellingen {
    var id: Long? = null

    var type: SignaleringType.Type? = null

    var subjecttype: SignaleringSubject? = null

    var dashboard: Boolean? = null

    var mail: Boolean? = null
}
