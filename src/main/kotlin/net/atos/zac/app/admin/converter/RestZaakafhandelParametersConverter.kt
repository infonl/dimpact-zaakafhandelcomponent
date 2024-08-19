/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.app.admin.model.RestZaakafhandelParameters
import net.atos.zac.app.zaak.converter.RESTResultaattypeConverter
import net.atos.zac.app.zaak.model.RESTZaakStatusmailOptie
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestZaakafhandelParametersConverter @Inject constructor(
    val caseDefinitionConverter: RESTCaseDefinitionConverter,
    val resultaattypeConverter: RESTResultaattypeConverter,
    val zaakbeeindigParameterConverter: RESTZaakbeeindigParameterConverter,
    val humanTaskParametersConverter: RESTHumanTaskParametersConverter,
    val ztcClientService: ZtcClientService,
    val zaakafhandelParameterService: ZaakafhandelParameterService
) {
    fun convertZaakafhandelParameters(
        zaakafhandelParameters: ZaakafhandelParameters,
        inclusiefRelaties: Boolean
    ): RestZaakafhandelParameters {
        val restZaakafhandelParameters = RestZaakafhandelParameters(
            id = zaakafhandelParameters.id,
            zaaktype = ztcClientService.readZaaktype(zaakafhandelParameters.zaakTypeUUID).let {
                RESTZaaktypeOverzichtConverter.convert(it)
            },
            defaultGroepId = zaakafhandelParameters.groepID,
            defaultBehandelaarId = zaakafhandelParameters.gebruikersnaamMedewerker,
            einddatumGeplandWaarschuwing = zaakafhandelParameters.einddatumGeplandWaarschuwing,
            uiterlijkeEinddatumAfdoeningWaarschuwing = zaakafhandelParameters
                .uiterlijkeEinddatumAfdoeningWaarschuwing,
            creatiedatum = zaakafhandelParameters.creatiedatum,
            valide = zaakafhandelParameters.isValide,
            caseDefinition = zaakafhandelParameters.caseDefinitionID?.let {
                caseDefinitionConverter.convertToRESTCaseDefinition(it, inclusiefRelaties)
            },
            intakeMail = zaakafhandelParameters.intakeMail?.let { RESTZaakStatusmailOptie.valueOf(it) },
            afrondenMail = zaakafhandelParameters.afrondenMail?.let { RESTZaakStatusmailOptie.valueOf(it) },
            productaanvraagtype = zaakafhandelParameters.productaanvraagtype,
            domein = zaakafhandelParameters.domein
        )
        restZaakafhandelParameters.caseDefinition?.takeIf { inclusiefRelaties }?.let { caseDefinition ->
            zaakafhandelParameters.nietOntvankelijkResultaattype?.let {
                ztcClientService.readResultaattype(it).let { resultaatType ->
                    restZaakafhandelParameters.zaakNietOntvankelijkResultaattype =
                        resultaattypeConverter.convertResultaattype(resultaatType)
                }
            }
            restZaakafhandelParameters.humanTaskParameters =
                humanTaskParametersConverter.convertHumanTaskParametersCollection(
                    zaakafhandelParameters.humanTaskParametersCollection,
                    caseDefinition.humanTaskDefinitions
                )
            restZaakafhandelParameters.userEventListenerParameters = RESTUserEventListenerParametersConverter
                .convertUserEventListenerParametersCollection(
                    zaakafhandelParameters.userEventListenerParametersCollection,
                    caseDefinition.userEventListenerDefinitions
                )
            restZaakafhandelParameters.zaakbeeindigParameters =
                zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaakafhandelParameters.zaakbeeindigParameters)
            restZaakafhandelParameters.mailtemplateKoppelingen = RESTMailtemplateKoppelingConverter.convert(
                zaakafhandelParameters.mailtemplateKoppelingen
            )
            restZaakafhandelParameters.zaakAfzenders = RESTZaakAfzenderConverter.convertZaakAfzenders(
                zaakafhandelParameters.zaakAfzenders
            )
        }
        return restZaakafhandelParameters
    }

    fun convertRESTZaakafhandelParameters(
        restZaakafhandelParameters: RestZaakafhandelParameters
    ): ZaakafhandelParameters {
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
            restZaakafhandelParameters.zaaktype.uuid
        )
        zaakafhandelParameters.id = restZaakafhandelParameters.id
        zaakafhandelParameters.zaakTypeUUID = restZaakafhandelParameters.zaaktype.uuid
        zaakafhandelParameters.zaaktypeOmschrijving = restZaakafhandelParameters.zaaktype.omschrijving
        zaakafhandelParameters.caseDefinitionID = restZaakafhandelParameters.caseDefinition!!.key
        zaakafhandelParameters.groepID = restZaakafhandelParameters.defaultGroepId
        zaakafhandelParameters.uiterlijkeEinddatumAfdoeningWaarschuwing =
            restZaakafhandelParameters.uiterlijkeEinddatumAfdoeningWaarschuwing
        zaakafhandelParameters.nietOntvankelijkResultaattype =
            restZaakafhandelParameters.zaakNietOntvankelijkResultaattype!!.id
        zaakafhandelParameters.intakeMail = restZaakafhandelParameters.intakeMail!!.name
        zaakafhandelParameters.afrondenMail = restZaakafhandelParameters.afrondenMail!!.name
        zaakafhandelParameters.productaanvraagtype = restZaakafhandelParameters.productaanvraagtype
        zaakafhandelParameters.domein = restZaakafhandelParameters.domein
        zaakafhandelParameters.gebruikersnaamMedewerker = restZaakafhandelParameters.defaultBehandelaarId
        if (restZaakafhandelParameters.einddatumGeplandWaarschuwing != null) {
            zaakafhandelParameters.einddatumGeplandWaarschuwing =
                restZaakafhandelParameters.einddatumGeplandWaarschuwing
        }
        zaakafhandelParameters.setHumanTaskParametersCollection(
            humanTaskParametersConverter.convertRESTHumanTaskParameters(
                restZaakafhandelParameters.humanTaskParameters
            )
        )
        zaakafhandelParameters.setUserEventListenerParametersCollection(
            RESTUserEventListenerParametersConverter.convertRESTUserEventListenerParameters(
                restZaakafhandelParameters.userEventListenerParameters
            )
        )
        zaakafhandelParameters.setZaakbeeindigParameters(
            zaakbeeindigParameterConverter.convertRESTZaakbeeindigParameters(
                restZaakafhandelParameters.zaakbeeindigParameters
            )
        )
        zaakafhandelParameters.setMailtemplateKoppelingen(
            RESTMailtemplateKoppelingConverter.convertRESTmailtemplateKoppelingen(
                restZaakafhandelParameters.mailtemplateKoppelingen
            )
        )
        zaakafhandelParameters.setZaakAfzenders(
            RESTZaakAfzenderConverter.convertRESTZaakAfzenders(restZaakafhandelParameters.zaakAfzenders)
        )
        return zaakafhandelParameters
    }
}
