/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering.model

import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import org.flowable.task.api.TaskInfo
import java.time.ZonedDateTime

@Suppress("LongParameterList")
fun createSignalering(
    type: SignaleringType = createSignaleringType(),
    zaak: Zaak? = createZaak(),
    taskInfo: TaskInfo? = null,
    enkelvoudigInformatieObject: EnkelvoudigInformatieObject? = null,
    tijdstip: ZonedDateTime = ZonedDateTime.now(),
    targetUser: User? = null,
    targetGroup: Group? = null
) = Signalering().apply {
    // note that type needs to be set first because other setters rely on it being set
    this.type = type
    enkelvoudigInformatieObject?.let { this.setSubject(it) }
    this.tijdstip = tijdstip
    taskInfo?.let { this.setSubject(it) }
    zaak?.let { this.setSubject(zaak) }
    targetUser?.let { this.setTarget(targetUser) }
    targetGroup?.let { this.setTarget(targetGroup) }
}

fun createSignaleringType(
    type: SignaleringType.Type = SignaleringType.Type.ZAAK_OP_NAAM,
    subjecttype: SignaleringSubject = SignaleringSubject.ZAAK,
) = SignaleringType().apply {
    this.type = type
    this.subjecttype = subjecttype
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

fun createSignaleringZoekParameters(
    signaleringSubject: SignaleringSubject = SignaleringSubject.ZAAK,
    subject: String = "dummySubject"
) = SignaleringZoekParameters(
    signaleringSubject,
    subject
)
