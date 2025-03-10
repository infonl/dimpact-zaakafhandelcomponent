/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering.model;

import static net.atos.zac.signalering.model.SignaleringSubject.DOCUMENT;
import static net.atos.zac.signalering.model.SignaleringSubject.TAAK;
import static net.atos.zac.signalering.model.SignaleringSubject.ZAAK;
import static net.atos.zac.signalering.model.SignaleringTarget.GROUP;
import static net.atos.zac.signalering.model.SignaleringTarget.USER;
import static net.atos.zac.util.FlywayIntegrator.SCHEMA;
import static nl.info.client.zgw.util.UriUtilsKt.extractUuid;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.flowable.task.api.TaskInfo;

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.zac.identity.model.Group;
import net.atos.zac.identity.model.User;

/**
 * Construction is easiest with the factory method in SignaleringService.
 */
@Entity
@Table(schema = SCHEMA, name = "signalering")
@SequenceGenerator(schema = SCHEMA, name = "sq_signalering", sequenceName = "sq_signalering", allocationSize = 1)
public class Signalering {
    @Id
    @GeneratedValue(generator = "sq_signalering", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_signalering")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "signaleringtype_enum", referencedColumnName = "signaleringtype_enum", nullable = false)
    private SignaleringType type;

    @NotNull @Column(name = "targettype_enum", nullable = false)
    @Enumerated(EnumType.STRING)
    private SignaleringTarget targettype;

    @NotBlank @Column(name = "target", nullable = false)
    private String target;

    @NotBlank @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "detail")
    private String detail;

    @NotNull @Column(name = "tijdstip", nullable = false)
    private ZonedDateTime tijdstip;

    public Long getId() {
        return id;
    }

    public SignaleringType getType() {
        return type;
    }

    public void setType(final SignaleringType type) {
        this.type = type;
    }

    public SignaleringTarget getTargettype() {
        return targettype;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(final Group group) {
        setTargetGroup(group.getId());
    }

    public void setTarget(final User user) {
        setTargetUser(user.getId());
    }

    public void setTargetGroup(final String target) {
        this.targettype = GROUP;
        this.target = target;
    }

    public void setTargetUser(final String target) {
        this.targettype = USER;
        this.target = target;
    }

    public SignaleringSubject getSubjecttype() {
        return getType().getSubjecttype();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final Zaak subject) {
        validSubjecttype(ZAAK);
        this.subject = subject.getUuid().toString();
    }

    public void setSubject(final TaskInfo subject) {
        validSubjecttype(TAAK);
        this.subject = subject.getId();
    }

    public void setSubject(final EnkelvoudigInformatieObject subject) {
        validSubjecttype(DOCUMENT);
        this.subject = extractUuid(subject.getUrl()).toString();
    }

    private void validSubjecttype(final SignaleringSubject subjecttype) {
        if (type.getSubjecttype() != subjecttype) {
            throw new IllegalArgumentException(
                    String.format("SignaleringType %s expects a %s-type subject", type, type.getSubjecttype()));
        }
    }

    public String getDetail() {
        return detail;
    }

    public void setDetailFromSignaleringDetail(final SignaleringDetail signaleringDetail) {
        this.detail = signaleringDetail.name();
    }

    public void setDetailFromZaakInformatieobject(final ZaakInformatieobject zaakInformatieobject) {
        this.detail = extractUuid(zaakInformatieobject.getInformatieobject()).toString();
    }

    public ZonedDateTime getTijdstip() {
        return tijdstip;
    }

    public void setTijdstip(final ZonedDateTime tijdstip) {
        this.tijdstip = tijdstip;
    }

    @Override
    public String toString() {
        return String.format("%s-signalering voor %s %s (over %s %s)", getType(), getTargettype(), getTarget(),
                getSubjecttype(), getSubject());
    }
}
