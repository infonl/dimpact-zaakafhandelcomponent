/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.converter;

import jakarta.inject.Inject;

import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.app.informatieobjecten.model.RESTZaakInformatieobject;
import net.atos.zac.app.policy.converter.RESTRechtenConverter;
import net.atos.zac.app.zaken.converter.RESTZaakStatusConverter;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.policy.output.ZaakRechten;

public class RESTZaakInformatieobjectConverter {

    @Inject private ZTCClientService ztcClientService;

    @Inject private ZRCClientService zrcClientService;

    @Inject private RESTZaakStatusConverter restZaakStatusConverter;

    @Inject private RESTRechtenConverter rechtenConverter;

    @Inject private PolicyService policyService;

    public RESTZaakInformatieobject convert(final ZaakInformatieobject zaakInformatieObject) {
        final Zaak zaak = zrcClientService.readZaak(zaakInformatieObject.getZaak());
        final ZaakType zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype());
        final ZaakRechten zaakrechten = policyService.readZaakRechten(zaak, zaaktype);
        final RESTZaakInformatieobject restZaakInformatieobject = new RESTZaakInformatieobject();
        restZaakInformatieobject.zaakIdentificatie = zaak.getIdentificatie();
        restZaakInformatieobject.zaakRechten = rechtenConverter.convert(zaakrechten);
        if (zaakrechten.lezen()) {
            restZaakInformatieobject.zaakStartDatum = zaak.getStartdatum();
            restZaakInformatieobject.zaakEinddatumGepland = zaak.getEinddatumGepland();
            restZaakInformatieobject.zaaktypeOmschrijving = zaaktype.getOmschrijving();
            if (zaak.getStatus() != null) {
                final Status status = zrcClientService.readStatus(zaak.getStatus());
                final StatusType statustype =
                        ztcClientService.readStatustype(status.getStatustype());
                restZaakInformatieobject.zaakStatus =
                        restZaakStatusConverter.convertToRESTZaakStatus(status, statustype);
            }
        }
        return restZaakInformatieobject;
    }
}
