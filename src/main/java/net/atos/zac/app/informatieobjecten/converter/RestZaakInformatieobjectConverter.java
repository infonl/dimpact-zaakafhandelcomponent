/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.converter;

import static nl.info.zac.app.zaak.model.RestZaakStatusKt.toRestZaakStatus;

import jakarta.inject.Inject;

import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.zac.app.informatieobjecten.model.RestZaakInformatieobject;
import net.atos.zac.app.policy.converter.RestRechtenConverter;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.policy.output.ZaakRechten;
import nl.info.client.zgw.ztc.ZtcClientService;
import nl.info.client.zgw.ztc.model.generated.StatusType;
import nl.info.client.zgw.ztc.model.generated.ZaakType;

public class RestZaakInformatieobjectConverter {

    @Inject
    private ZtcClientService ztcClientService;

    @Inject
    private ZrcClientService zrcClientService;

    @Inject
    private PolicyService policyService;

    public RestZaakInformatieobject convert(final ZaakInformatieobject zaakInformatieObject) {
        final Zaak zaak = zrcClientService.readZaak(zaakInformatieObject.getZaak());
        final ZaakType zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype());
        final ZaakRechten zaakrechten = policyService.readZaakRechten(zaak, zaaktype);
        final RestZaakInformatieobject restZaakInformatieobject = new RestZaakInformatieobject();
        restZaakInformatieobject.zaakIdentificatie = zaak.getIdentificatie();
        restZaakInformatieobject.zaakRechten = RestRechtenConverter.convert(zaakrechten);
        if (zaakrechten.lezen()) {
            restZaakInformatieobject.zaakStartDatum = zaak.getStartdatum();
            restZaakInformatieobject.zaakEinddatumGepland = zaak.getEinddatumGepland();
            restZaakInformatieobject.zaaktypeOmschrijving = zaaktype.getOmschrijving();
            if (zaak.getStatus() != null) {
                final Status status = zrcClientService.readStatus(zaak.getStatus());
                final StatusType statustype = ztcClientService.readStatustype(status.getStatustype());
                restZaakInformatieobject.zaakStatus = toRestZaakStatus(status, statustype);
            }
        }
        return restZaakInformatieobject;
    }
}
