/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.mail;

import static net.atos.client.zgw.zrc.util.StatusTypeUtil.isHeropend;
import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.mailjet.client.errors.MailjetException;

import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter;
import net.atos.zac.app.mail.model.RESTMailGegevens;
import net.atos.zac.flowable.ZaakVariabelenService;
import net.atos.zac.mail.MailService;
import net.atos.zac.mail.model.Bronnen;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.util.ValidationUtil;

@Singleton
@Path("mail")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailRESTService {

    @Inject
    private MailService mailService;

    @Inject
    private ZaakVariabelenService zaakVariabelenService;

    @Inject
    private PolicyService policyService;

    @Inject
    private ZRCClientService zrcClientService;

    @Inject
    private ZTCClientService ztcClientService;

    @Inject
    private RESTMailGegevensConverter restMailGegevensConverter;

    @POST
    @Path("send/{zaakUuid}")
    public void sendMail(
            @PathParam("zaakUuid") final UUID zaakUUID,
            final RESTMailGegevens restMailGegevens
    ) throws MailjetException {
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        assertPolicy(policyService.readZaakRechten(zaak).versturenEmail());
        validateEmail(restMailGegevens.verzender);
        validateEmail(restMailGegevens.ontvanger);
        mailService.sendMail(
                restMailGegevensConverter.convert(restMailGegevens), Bronnen.fromZaak(zaak));
    }

    @POST
    @Path("acknowledge/{zaakUuid}")
    public void sendAcknowledgmentReceiptMail(
            @PathParam("zaakUuid") final UUID zaakUuid,
            final RESTMailGegevens restMailGegevens
    ) throws MailjetException {
        final Zaak zaak = zrcClientService.readZaak(zaakUuid);
        assertPolicy(!zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.getUuid()).orElse(false) &&
                     policyService.readZaakRechten(zaak).versturenOntvangstbevestiging());
        validateEmail(restMailGegevens.verzender);
        validateEmail(restMailGegevens.ontvanger);
        mailService.sendMail(
                restMailGegevensConverter.convert(restMailGegevens), Bronnen.fromZaak(zaak));

        final StatusType statustype = zaak.getStatus() != null ?
                ztcClientService.readStatustype(zrcClientService.readStatus(zaak.getStatus()).getStatustype()) : null;
        if (!isHeropend(statustype)) {
            zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaakUuid, Boolean.TRUE);
        }
    }

    private void validateEmail(final String email) {
        if (!ValidationUtil.isValidEmail(email)) {
            throw new RuntimeException(String.format("E-Mail '%s' is not valid", email));
        }
    }
}
