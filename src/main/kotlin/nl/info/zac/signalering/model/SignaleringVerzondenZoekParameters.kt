/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.signalering.model.SignaleringSubject.DOCUMENT
import nl.info.zac.signalering.model.SignaleringSubject.TAAK
import nl.info.zac.signalering.model.SignaleringSubject.ZAAK
import nl.info.zac.signalering.model.SignaleringTarget.GROUP
import nl.info.zac.signalering.model.SignaleringTarget.USER
import org.flowable.task.api.TaskInfo
import java.util.EnumSet
import java.util.UUID

class SignaleringVerzondenZoekParameters {
    var targettype: SignaleringTarget? = null
        private set

    var target: String? = null
        private set

    var types: Set<SignaleringType.Type> = emptySet()
        private set

    var subjecttype: SignaleringSubject? = null
        private set

    var subject: String? = null
        private set

    var detail: SignaleringDetail? = null
        private set

    constructor(targettype: SignaleringTarget, target: String) {
        this.targettype = targettype
        this.target = target
    }

    constructor(subjecttype: SignaleringSubject, subject: String) {
        this.subjecttype = subjecttype
        this.subject = subject
    }

    constructor(target: Group) : this(GROUP, target.name)

    constructor(target: User) : this(USER, target.id)

    fun types(vararg types: SignaleringType.Type) = apply { this.types = EnumSet.copyOf(types.asList()) }

    fun subjecttype(subjecttype: SignaleringSubject) = apply { this.subjecttype = subjecttype }

    fun subject(subject: Zaak) = subjectZaak(subject.uuid)

    fun subject(subject: TaskInfo) = subjectTaak(subject.id)

    fun subject(subject: EnkelvoudigInformatieObject) = subjectInformatieobject(subject.url.extractUuid())

    fun subjectZaak(zaakId: UUID) = apply { subject = zaakId.toString() }.subjecttype(ZAAK)

    fun subjectTaak(taakId: String) = apply { subject = taakId }.subjecttype(TAAK)

    fun subjectInformatieobject(informatieobjectId: UUID) =
        apply { subject = informatieobjectId.toString() }.subjecttype(DOCUMENT)

    fun detail(detail: SignaleringDetail) = apply { this.detail = detail }
}
