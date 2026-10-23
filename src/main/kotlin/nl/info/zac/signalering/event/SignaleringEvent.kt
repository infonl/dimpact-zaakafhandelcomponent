/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.event

import nl.info.zac.event.AbstractEvent
import nl.info.zac.event.Opcode
import nl.info.zac.identity.model.User
import nl.info.zac.signalering.model.SignaleringType
import java.io.Serial

/**
 * @param objectType the operation that happened on the referenced object (the opcode is always UPDATED for these events)
 * @param objectId the identification of the object the operation was done on
 * @param actor the user that initiated the operation directly or indirectly
 */
class SignaleringEvent<ID : Any>(
    override val objectType: SignaleringType.Type,
    objectId: SignaleringEventId<ID>,
    actor: User?
) : AbstractEvent<SignaleringType.Type, SignaleringEventId<ID>>(Opcode.UPDATED, objectId) {
    companion object {
        @Serial
        private const val serialVersionUID = 184493471780916087L
    }

    val actor: String? = actor?.id

    override fun toString() = "${super.toString()} $actor"
}
