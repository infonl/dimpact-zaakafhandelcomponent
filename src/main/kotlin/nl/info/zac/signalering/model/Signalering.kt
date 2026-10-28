/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.signalering.model.SignaleringSubject.DOCUMENT
import nl.info.zac.signalering.model.SignaleringSubject.TAAK
import nl.info.zac.signalering.model.SignaleringSubject.ZAAK
import nl.info.zac.signalering.model.SignaleringTarget.GROUP
import nl.info.zac.signalering.model.SignaleringTarget.USER
import nl.info.zac.util.AllOpen
import org.flowable.task.api.TaskInfo
import java.time.ZonedDateTime

/**
 * Construction is easiest with the factory method in SignaleringService.
 */
@Entity
@Table(schema = SCHEMA, name = "signalering")
@SequenceGenerator(schema = SCHEMA, name = "sq_signalering", sequenceName = "sq_signalering", allocationSize = 1)
@AllOpen
class Signalering {
    @Id
    @GeneratedValue(generator = "sq_signalering", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_signalering")
    var id: Long? = null
        protected set

    @NotNull
    @ManyToOne
    @JoinColumn(name = "signaleringtype_enum", referencedColumnName = "signaleringtype_enum", nullable = false)
    lateinit var type: SignaleringType

    @NotNull
    @Column(name = "targettype_enum", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var targettype: SignaleringTarget
        protected set

    @NotBlank
    @Column(name = "target", nullable = false)
    lateinit var target: String
        protected set

    @NotBlank
    @Column(name = "subject", nullable = false)
    lateinit var subject: String
        protected set

    @Column(name = "detail")
    var detail: String? = null
        protected set

    @NotNull
    @Column(name = "tijdstip", nullable = false)
    lateinit var tijdstip: ZonedDateTime

    val subjecttype: SignaleringSubject
        get() = type.subjecttype

    fun setTarget(group: Group) = setTargetGroup(group.name)

    fun setTarget(user: User) = setTargetUser(user.username)

    fun setTargetGroup(target: String) {
        targettype = GROUP
        this.target = target
    }

    fun setTargetUser(target: String) {
        targettype = USER
        this.target = target
    }

    fun setSubject(subject: Zaak) {
        validSubjecttype(ZAAK)
        this.subject = subject.uuid.toString()
    }

    fun setSubject(subject: TaskInfo) {
        validSubjecttype(TAAK)
        this.subject = subject.id
    }

    fun setSubject(subject: EnkelvoudigInformatieObject) {
        validSubjecttype(DOCUMENT)
        this.subject = subject.url.extractUuid().toString()
    }

    private fun validSubjecttype(subjecttype: SignaleringSubject) =
        require(type.subjecttype == subjecttype) {
            "SignaleringType $type expects a ${type.subjecttype}-type subject"
        }

    fun setDetailFromSignaleringDetail(signaleringDetail: SignaleringDetail) {
        detail = signaleringDetail.name
    }

    fun setDetailFromZaakInformatieobject(zaakInformatieobject: ZaakInformatieObject) {
        detail = zaakInformatieobject.informatieobject.extractUuid().toString()
    }

    override fun toString() = "$type-signalering voor $targettype $target (over $subjecttype $subject)"
}
