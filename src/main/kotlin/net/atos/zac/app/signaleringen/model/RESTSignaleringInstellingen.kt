/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signaleringen.model

import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringType

class RESTSignaleringInstellingen {
    @JvmField
    var id: Long? = null

    @JvmField
    var type: SignaleringType.Type? = null

    @JvmField
    var subjecttype: SignaleringSubject? = null

    @JvmField
    var dashboard: Boolean? = null

    @JvmField
    var mail: Boolean? = null
}
