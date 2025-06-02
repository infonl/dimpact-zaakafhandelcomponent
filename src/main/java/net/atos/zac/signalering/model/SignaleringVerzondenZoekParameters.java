/*
 * SPDX-FileCopyrightText: 2022 Atos
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

public class SignaleringVerzondenZoekParameters {
    private SignaleringTarget targettype;

    private String target;

    private Set<SignaleringType.Type> types;

    private SignaleringSubject subjecttype;

    private String subject;

    private SignaleringDetail detail;

    public SignaleringVerzondenZoekParameters(final SignaleringTarget targettype, final String target) {
        this.targettype = targettype;
        this.target = target;
    }

    public SignaleringVerzondenZoekParameters(final SignaleringSubject subjectType, final String subject) {
        this.subjecttype = subjectType;
        this.subject = subject;
    }

    public SignaleringVerzondenZoekParameters(final Group target) {
        this(GROUP, target.getId());
    }

    public SignaleringVerzondenZoekParameters(final User target) {
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

    public SignaleringVerzondenZoekParameters types(final SignaleringType.Type... types) {
        this.types = EnumSet.copyOf(Arrays.asList(types));
        return this;
    }

    public SignaleringVerzondenZoekParameters types(final SignaleringType.Type type) {
        this.types = EnumSet.of(type);
        return this;
    }

    public SignaleringSubject getSubjecttype() {
        return subjecttype;
    }

    public String getSubject() {
        return subject;
    }

    public SignaleringVerzondenZoekParameters subjecttype(final SignaleringSubject subjecttype) {
        this.subjecttype = subjecttype;
        return this;
    }

    public SignaleringVerzondenZoekParameters subject(final Zaak subject) {
        return subjectZaak(subject.getUuid());
    }

    public SignaleringVerzondenZoekParameters subject(final TaskInfo subject) {
        return subjectTaak(subject.getId());
    }

    public SignaleringVerzondenZoekParameters subject(final EnkelvoudigInformatieObject subject) {
        return subjectInformatieobject(extractUuid(subject.getUrl()));
    }

    public SignaleringVerzondenZoekParameters subjectZaak(final UUID zaakId) {
        this.subject = zaakId.toString();
        return subjecttype(ZAAK);
    }

    public SignaleringVerzondenZoekParameters subjectTaak(final String taakId) {
        this.subject = taakId;
        return subjecttype(TAAK);
    }

    public SignaleringVerzondenZoekParameters subjectInformatieobject(final UUID informatieobjectId) {
        this.subject = informatieobjectId.toString();
        return subjecttype(DOCUMENT);
    }

    public SignaleringDetail getDetail() {
        return detail;
    }

    public SignaleringVerzondenZoekParameters detail(final SignaleringDetail detail) {
        this.detail = detail;
        return this;
    }
}
