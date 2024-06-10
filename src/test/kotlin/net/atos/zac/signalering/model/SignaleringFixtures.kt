/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering.model

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createZaak
import org.flowable.task.api.TaskInfo

fun createSignalering(
    type: SignaleringType = SignaleringType().apply {
        this.type = SignaleringType.Type.ZAAK_OP_NAAM
        this.subjecttype = SignaleringSubject.ZAAK
    },
    zaak: Zaak? = createZaak(),
    taskInfo: TaskInfo? = null,
    enkelvoudigInformatieObject: EnkelvoudigInformatieObject? = null,
) = Signalering().apply {
    this.type = type
    zaak?.let { this.setSubject(zaak) }
    taskInfo?.let { this.setSubject(taskInfo) }
    enkelvoudigInformatieObject?.let { this.setSubject(enkelvoudigInformatieObject) }
}

@Suppress("LongParameterList")
fun createSignaleringInstellingen(
    id: Long = 1234L,
    type: SignaleringType = SignaleringType().apply {
        this.type = SignaleringType.Type.ZAAK_OP_NAAM
        this.subjecttype = SignaleringSubject.ZAAK
    },
    ownerType: SignaleringTarget = SignaleringTarget.USER,
    ownerId: String = "dummyMedewerker",
    isDashboard: Boolean = true,
    isMail: Boolean = true,
) =
    SignaleringInstellingen(
        type,
        ownerType,
        ownerId,
    ).apply {
        this.id = id
        this.isDashboard = isDashboard
        this.isMail = isMail
    }
