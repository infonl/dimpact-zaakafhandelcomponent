/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.enkelvoudiginformatieobject.model

import java.util.UUID

fun createEnkelvoudigInformatieObjectLock(
    enkelvoudigInformatieObjectUUID: UUID = UUID.randomUUID(),
    userId: String? = null,
    lock: String? = null
) = EnkelvoudigInformatieObjectLock().apply {
    enkelvoudiginformatieobjectUUID = enkelvoudigInformatieObjectUUID
    this.userId = userId
    this.lock = lock
}
