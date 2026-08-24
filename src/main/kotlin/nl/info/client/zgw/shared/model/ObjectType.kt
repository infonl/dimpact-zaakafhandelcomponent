/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.model;

import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.shared.model.audit.AuditWijziging;
import nl.info.client.zgw.shared.model.audit.besluiten.BesluitInformatieobjectWijziging;
import nl.info.client.zgw.shared.model.audit.besluiten.BesluitWijziging;
import nl.info.client.zgw.shared.model.audit.documenten.EnkelvoudigInformatieobjectWijziging;
import nl.info.client.zgw.shared.model.audit.documenten.GebruiksrechtenWijziging;
import nl.info.client.zgw.shared.model.audit.documenten.ObjectInformatieobjectWijziging;

public enum ObjectType {
    ENKELVOUDIG_INFORMATIEOBJECT("/documenten/api/v1/enkelvoudiginformatieobjecten/", EnkelvoudigInformatieobjectWijziging.class),

    GEBRUIKSRECHTEN("/documenten/api/v1/gebruiksrechten", GebruiksrechtenWijziging.class),

    OBJECT_INFORMATIEOBJECT("documenten/api/v1/objectinformatieobjecten", ObjectInformatieobjectWijziging.class),

    BESLUIT("/besluiten/api/v1/besluiten", BesluitWijziging.class),

    BESLUIT_INFORMATIEOBJECT("/besluiten/api/v1/besluitinformatieobjecten", BesluitInformatieobjectWijziging.class);

    private final String url;

    public final Type auditClass;

    ObjectType(final String url, final Class<? extends AuditWijziging<?>> classType) {
        this.url = url;
        this.auditClass = classType;
    }

    public static ObjectType getObjectType(String url) {
        for (ObjectType value : values()) {
            if (StringUtils.contains(url, value.url)) {
                return value;
            }
        }
        throw new RuntimeException(String.format("URL '%s' wordt niet ondersteund", url));
    }

    public Type getAuditClass() {
        return auditClass;
    }
}
