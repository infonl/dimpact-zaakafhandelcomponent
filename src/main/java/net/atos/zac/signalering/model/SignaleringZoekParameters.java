/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering.model;

import static net.atos.zac.signalering.model.SignaleringSubject.DOCUMENT;
import static net.atos.zac.signalering.model.SignaleringSubject.TAAK;
import static net.atos.zac.signalering.model.SignaleringSubject.ZAAK;
import static net.atos.zac.signalering.model.SignaleringTarget.GROUP;
import static net.atos.zac.signalering.model.SignaleringTarget.USER;
import static nl.info.client.zgw.util.ZgwUriUtilsKt.extractUuid;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.flowable.task.api.TaskInfo;

import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import nl.info.client.zgw.zrc.model.generated.Zaak;
import nl.info.zac.identity.model.Group;
import nl.info.zac.identity.model.User;

public class SignaleringZoekParameters {
    private SignaleringTarget targettype;

    private String target;

    private Set<SignaleringType.Type> types;

    private SignaleringSubject subjecttype;

    private String subject;

    private String detail;

    public SignaleringZoekParameters(final Signalering signalering) {
        this.targettype = signalering.getTargettype();
        this.target = signalering.getTarget();
        this.types = Set.of(signalering.getType().getType());
        this.subjecttype = signalering.getSubjecttype();
        this.subject = signalering.getSubject();
        this.detail = signalering.getDetail();
    }

    public SignaleringZoekParameters(final SignaleringTarget targettype, final String target) {
        this.targettype = targettype;
        this.target = target;
    }

    public SignaleringZoekParameters(final SignaleringSubject subjectType, final String subject) {
        this.subjecttype = subjectType;
        this.subject = subject;
    }

    public SignaleringZoekParameters(final Group target) {
        this(GROUP, target.getId());
    }

    public SignaleringZoekParameters(final User target) {
        this(USER, target.getId());
    }

    public SignaleringTarget getTargettype() {
        return targettype;
    }

    public String getTarget() {
        return target;
    }

    public Set<SignaleringType.Type> getTypes() {
        return types == null ? Collections.emptySet() : Collections.unmodifiableSet(types);
    }

    public SignaleringZoekParameters types(final SignaleringType.Type... types) {
        this.types = EnumSet.copyOf(Arrays.asList(types));
        return this;
    }

    public SignaleringZoekParameters types(final SignaleringType.Type type) {
        this.types = EnumSet.of(type);
        return this;
    }

    public SignaleringSubject getSubjecttype() {
        return subjecttype;
    }

    public String getSubject() {
        return subject;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public SignaleringZoekParameters subjecttype(final SignaleringSubject subjecttype) {
        this.subjecttype = subjecttype;
        return this;
    }

    public SignaleringZoekParameters subject(final Zaak subject) {
        return subjectZaak(subject.getUuid());
    }

    public SignaleringZoekParameters subject(final TaskInfo subject) {
        return subjectTaak(subject.getId());
    }

    public SignaleringZoekParameters subject(final EnkelvoudigInformatieObject subject) {
        return subjectInformatieobject(extractUuid(subject.getUrl()));
    }

    public SignaleringZoekParameters subjectZaak(final UUID zaakId) {
        this.subject = zaakId.toString();
        return subjecttype(ZAAK);
    }

    public SignaleringZoekParameters subjectTaak(final String taakId) {
        this.subject = taakId;
        return subjecttype(TAAK);
    }

    public SignaleringZoekParameters subjectInformatieobject(final UUID informatieobjectId) {
        this.subject = informatieobjectId.toString();
        return subjecttype(DOCUMENT);
    }
}
