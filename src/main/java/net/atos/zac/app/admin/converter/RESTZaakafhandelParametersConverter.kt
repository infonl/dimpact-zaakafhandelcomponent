/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import jakarta.inject.Inject;

import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.zac.admin.ZaakafhandelParameterService;
import net.atos.zac.admin.model.ZaakafhandelParameters;
import net.atos.zac.app.admin.model.RestZaakafhandelParameters;
import net.atos.zac.app.zaak.converter.RESTResultaattypeConverter;
import net.atos.zac.app.zaak.model.RESTZaakStatusmailOptie;

public class RESTZaakafhandelParametersConverter {

    @Inject
    private RESTCaseDefinitionConverter caseDefinitionConverter;

    @Inject
    private RESTResultaattypeConverter resultaattypeConverter;

    @Inject
    private RESTZaakbeeindigParameterConverter zaakbeeindigParameterConverter;

    @Inject
    private RESTHumanTaskParametersConverter humanTaskParametersConverter;

    @Inject
    private ZtcClientService ztcClientService;

    @Inject
    private ZaakafhandelParameterService zaakafhandelParameterService;

    public RestZaakafhandelParameters convertZaakafhandelParameters(
            final ZaakafhandelParameters zaakafhandelParameters,
            final boolean inclusiefRelaties
    ) {
        final RestZaakafhandelParameters restZaakafhandelParameters = new RestZaakafhandelParameters();
        restZaakafhandelParameters.id = zaakafhandelParameters.getId();
        restZaakafhandelParameters.zaaktype = RESTZaaktypeOverzichtConverter.convert(
                ztcClientService.readZaaktype(zaakafhandelParameters.getZaakTypeUUID())
        );
        restZaakafhandelParameters.defaultGroepId = zaakafhandelParameters.getGroepID();
        restZaakafhandelParameters.defaultBehandelaarId = zaakafhandelParameters.getGebruikersnaamMedewerker();
        restZaakafhandelParameters.einddatumGeplandWaarschuwing = zaakafhandelParameters.getEinddatumGeplandWaarschuwing();
        restZaakafhandelParameters.uiterlijkeEinddatumAfdoeningWaarschuwing = zaakafhandelParameters
                .getUiterlijkeEinddatumAfdoeningWaarschuwing();
        restZaakafhandelParameters.creatiedatum = zaakafhandelParameters.getCreatiedatum();
        restZaakafhandelParameters.valide = zaakafhandelParameters.isValide();

        if (zaakafhandelParameters.getCaseDefinitionID() != null) {
            restZaakafhandelParameters.caseDefinition = caseDefinitionConverter.convertToRESTCaseDefinition(
                    zaakafhandelParameters.getCaseDefinitionID(), inclusiefRelaties);
        }
        if (inclusiefRelaties && restZaakafhandelParameters.caseDefinition != null) {
            if (zaakafhandelParameters.getNietOntvankelijkResultaattype() != null) {
                restZaakafhandelParameters.zaakNietOntvankelijkResultaattype = resultaattypeConverter.convertResultaattype(
                        ztcClientService.readResultaattype(zaakafhandelParameters.getNietOntvankelijkResultaattype()));
            }
            restZaakafhandelParameters.humanTaskParameters = humanTaskParametersConverter.convertHumanTaskParametersCollection(
                    zaakafhandelParameters.getHumanTaskParametersCollection(),
                    restZaakafhandelParameters.caseDefinition.humanTaskDefinitions);
            restZaakafhandelParameters.userEventListenerParameters = RESTUserEventListenerParametersConverter
                    .convertUserEventListenerParametersCollection(
                            zaakafhandelParameters.getUserEventListenerParametersCollection(),
                            restZaakafhandelParameters.caseDefinition.userEventListenerDefinitions
                    );
            restZaakafhandelParameters.zaakbeeindigParameters = zaakbeeindigParameterConverter.convertZaakbeeindigParameters(
                    zaakafhandelParameters.getZaakbeeindigParameters()
            );
            restZaakafhandelParameters.mailtemplateKoppelingen = RESTMailtemplateKoppelingConverter.convert(
                    zaakafhandelParameters.getMailtemplateKoppelingen()
            );
            restZaakafhandelParameters.zaakAfzenders = RESTZaakAfzenderConverter.convertZaakAfzenders(
                    zaakafhandelParameters.getZaakAfzenders()
            );
        }
        if (zaakafhandelParameters.getIntakeMail() != null) {
            restZaakafhandelParameters.intakeMail = RESTZaakStatusmailOptie.valueOf(
                    zaakafhandelParameters.getIntakeMail());
        }
        if (zaakafhandelParameters.getAfrondenMail() != null) {
            restZaakafhandelParameters.afrondenMail = RESTZaakStatusmailOptie.valueOf(
                    zaakafhandelParameters.getAfrondenMail());
        }
        restZaakafhandelParameters.productaanvraagtype = zaakafhandelParameters.getProductaanvraagtype();
        restZaakafhandelParameters.domein = zaakafhandelParameters.getDomein();

        return restZaakafhandelParameters;
    }

    public ZaakafhandelParameters convertRESTZaakafhandelParameters(
            final RestZaakafhandelParameters restZaakafhandelParameters
    ) {
        final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                restZaakafhandelParameters.zaaktype.uuid
        );
        zaakafhandelParameters.setId(restZaakafhandelParameters.id);
        zaakafhandelParameters.setZaakTypeUUID(restZaakafhandelParameters.zaaktype.uuid);
        zaakafhandelParameters.setZaaktypeOmschrijving(restZaakafhandelParameters.zaaktype.omschrijving);
        zaakafhandelParameters.setCaseDefinitionID(restZaakafhandelParameters.caseDefinition.key);
        zaakafhandelParameters.setGroepID(restZaakafhandelParameters.defaultGroepId);
        zaakafhandelParameters.setUiterlijkeEinddatumAfdoeningWaarschuwing(
                restZaakafhandelParameters.uiterlijkeEinddatumAfdoeningWaarschuwing);
        zaakafhandelParameters.setNietOntvankelijkResultaattype(
                restZaakafhandelParameters.zaakNietOntvankelijkResultaattype.getId());
        zaakafhandelParameters.setIntakeMail(restZaakafhandelParameters.intakeMail.name());
        zaakafhandelParameters.setAfrondenMail(restZaakafhandelParameters.afrondenMail.name());
        zaakafhandelParameters.setProductaanvraagtype(restZaakafhandelParameters.productaanvraagtype);
        zaakafhandelParameters.setDomein(restZaakafhandelParameters.domein);
        zaakafhandelParameters.setGebruikersnaamMedewerker(restZaakafhandelParameters.defaultBehandelaarId);
        if (restZaakafhandelParameters.einddatumGeplandWaarschuwing != null) {
            zaakafhandelParameters.setEinddatumGeplandWaarschuwing(
                    restZaakafhandelParameters.einddatumGeplandWaarschuwing);
        }
        zaakafhandelParameters.setHumanTaskParametersCollection(
                humanTaskParametersConverter.convertRESTHumanTaskParameters(
                        restZaakafhandelParameters.humanTaskParameters));
        zaakafhandelParameters.setUserEventListenerParametersCollection(
                RESTUserEventListenerParametersConverter.convertRESTUserEventListenerParameters(
                        restZaakafhandelParameters.userEventListenerParameters));
        zaakafhandelParameters.setZaakbeeindigParameters(
                zaakbeeindigParameterConverter.convertRESTZaakbeeindigParameters(
                        restZaakafhandelParameters.zaakbeeindigParameters));
        zaakafhandelParameters.setMailtemplateKoppelingen(
                RESTMailtemplateKoppelingConverter.convertRESTmailtemplateKoppelingen(
                        restZaakafhandelParameters.mailtemplateKoppelingen));
        zaakafhandelParameters.setZaakAfzenders(
                RESTZaakAfzenderConverter.convertRESTZaakAfzenders(restZaakafhandelParameters.zaakAfzenders)
        );
        return zaakafhandelParameters;
    }
}
