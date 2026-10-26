/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.signalering.model.SignaleringTarget.GROUP
import nl.info.zac.signalering.model.SignaleringTarget.USER

class SignaleringInstellingenZoekParameters(val ownertype: SignaleringTarget, val owner: String) {
    var type: SignaleringType.Type? = null
        private set

    var isDashboard: Boolean = false
        private set

    var isMail: Boolean = false
        private set

    constructor(signalering: Signalering) : this(signalering.targettype, signalering.target) {
        type = signalering.type.type
    }

    constructor(owner: Group) : this(GROUP, owner.name)

    constructor(owner: User) : this(USER, owner.username)

    fun type(type: SignaleringType.Type) = apply { this.type = type }

    fun dashboard() = apply { isDashboard = true }

    fun mail() = apply { isMail = true }
}
