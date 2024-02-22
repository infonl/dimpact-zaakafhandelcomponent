/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mailtemplate;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.app.admin.converter.RESTMailtemplateConverter;
import net.atos.zac.app.admin.model.RESTMailtemplate;
import net.atos.zac.mailtemplates.MailTemplateService;
import net.atos.zac.mailtemplates.model.Mail;
import net.atos.zac.util.UriUtil;
import net.atos.zac.zaaksturing.ZaakafhandelParameterService;
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters;

@Singleton
@Path("mailtemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailtemplateRESTService {

    @Inject private MailTemplateService mailTemplateService;

    @Inject private RESTMailtemplateConverter restMailtemplateConverter;

    @Inject private ZaakafhandelParameterService zaakafhandelParameterService;

    @Inject private ZRCClientService zrcClientService;

    @GET
    @Path("{mailtemplateEnum}/{zaakUUID}")
    public RESTMailtemplate findMailtemplate(
            @PathParam("mailtemplateEnum") final Mail mail,
            @PathParam("zaakUUID") final UUID zaakUUID) {
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        final ZaakafhandelParameters zaakafhandelParameters =
                zaakafhandelParameterService.readZaakafhandelParameters(
                        UriUtil.uuidFromURI(zaak.getZaaktype()));

        return zaakafhandelParameters.getMailtemplateKoppelingen().stream()
                .filter(koppeling -> koppeling.getMailTemplate().getMail().equals(mail))
                .map(koppeling -> restMailtemplateConverter.convert(koppeling.getMailTemplate()))
                .findFirst()
                .orElseGet(
                        () ->
                                mailTemplateService
                                        .findDefaultMailtemplate(mail)
                                        .map(restMailtemplateConverter::convert)
                                        .orElse(null));
    }
}
