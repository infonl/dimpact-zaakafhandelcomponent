/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.audit.converter.zaken;

import java.util.stream.Stream;

import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.model.audit.AuditWijziging;
import net.atos.client.zgw.zrc.model.Objecttype;
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag;
import net.atos.zac.app.audit.converter.AbstractAuditWijzigingConverter;
import net.atos.zac.app.audit.model.RESTHistorieRegel;

public class AuditZaakobjectWijzigingConverter extends AbstractAuditWijzigingConverter<AuditWijziging<Zaakobject>> {

    @Override
    public boolean supports(final ObjectType objectType) {
        return ObjectType.ZAAKOBJECT == objectType;
    }

    @Override
    protected Stream<RESTHistorieRegel> doConvert(final AuditWijziging<Zaakobject> wijziging) {
        var nieuw = wijziging.getNieuw();

        if (nieuw instanceof ZaakobjectProductaanvraag)
            return Stream.empty();

        var oud = wijziging.getOud();

        return Stream.of(new RESTHistorieRegel(toAttribuutLabel(wijziging), toWaarde(oud), toWaarde(nieuw)));
    }

    private String toAttribuutLabel(final AuditWijziging<Zaakobject> wijziging) {
        final Objecttype objecttype;
        if (wijziging.getOud() != null) {
            objecttype = wijziging.getOud().getObjectType();
        } else {
            objecttype = wijziging.getNieuw().getObjectType();
        }
        return "objecttype." + objecttype.name();
    }

    private String toWaarde(final Zaakobject zaakobject) {
        if (zaakobject == null) {
            return null;
        }
        return zaakobject.getWaarde();
    }

}
