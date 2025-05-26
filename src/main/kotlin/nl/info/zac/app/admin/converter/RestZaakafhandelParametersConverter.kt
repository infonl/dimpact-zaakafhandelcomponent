/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTHumanTaskParametersConverter
import net.atos.zac.app.admin.converter.RESTMailtemplateKoppelingConverter
import net.atos.zac.app.admin.converter.RESTMailtemplateKoppelingConverter.convertRESTmailtemplateKoppelingen
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter.convertRESTUserEventListenerParameters
import net.atos.zac.app.admin.converter.RESTZaakAfzenderConverter
import net.atos.zac.app.admin.converter.RESTZaakAfzenderConverter.convertRESTZaakAfzenders
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter.convertRESTZaakbeeindigParameters
import net.atos.zac.app.admin.converter.RESTZaaktypeOverzichtConverter
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.admin.model.RestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.RestBrpDoelbindingen
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.admin.model.RestZaakafhandelParameters
import nl.info.zac.app.admin.model.toBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toBrpDoelbindingen
import nl.info.zac.app.admin.model.toRestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toRestBrpDoelbindingen
import nl.info.zac.app.zaak.model.RESTZaakStatusmailOptie
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
@Suppress("LongParameterList")
class RestZaakafhandelParametersConverter @Inject constructor(
    val caseDefinitionConverter: RESTCaseDefinitionConverter,
    val zaakbeeindigParameterConverter: RESTZaakbeeindigParameterConverter,
    val humanTaskParametersConverter: RESTHumanTaskParametersConverter,
    val ztcClientService: ZtcClientService,
    val zaakafhandelParameterService: ZaakafhandelParameterService,
    val smartDocumentsService: SmartDocumentsService,
) {
    @Suppress("LongMethod")
    fun toRestZaakafhandelParameters(
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
            domein = zaakafhandelParameters.domein,
            smartDocuments = RestSmartDocuments(
                enabledGlobally = smartDocumentsService.isEnabled(),
                enabledForZaaktype = zaakafhandelParameters.isSmartDocumentsIngeschakeld
            ),
            betrokkeneKoppelingen = zaakafhandelParameters.betrokkeneKoppelingen
                ?.toRestBetrokkeneKoppelingen()
                ?: RestBetrokkeneKoppelingen(),
            brpDoelbindingen = zaakafhandelParameters.brpDoelbindingen
                ?.toRestBrpDoelbindingen()
                ?: RestBrpDoelbindingen(),
        )
        restZaakafhandelParameters.caseDefinition?.takeIf { inclusiefRelaties }?.let { caseDefinition ->
            zaakafhandelParameters.nietOntvankelijkResultaattype?.let {
                ztcClientService.readResultaattype(it).let { resultaatType ->
                    restZaakafhandelParameters.zaakNietOntvankelijkResultaattype = resultaatType.toRestResultaatType()
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
                zaakbeeindigParameterConverter.convertZaakbeeindigParameters(
                    zaakafhandelParameters.zaakbeeindigParameters
                )
            restZaakafhandelParameters.mailtemplateKoppelingen = RESTMailtemplateKoppelingConverter.convert(
                zaakafhandelParameters.mailtemplateKoppelingen
            )
            restZaakafhandelParameters.zaakAfzenders = RESTZaakAfzenderConverter.convertZaakAfzenders(
                zaakafhandelParameters.zaakAfzenders
            )
        }
        return restZaakafhandelParameters
    }

    fun toZaakafhandelParameters(
        restZaakafhandelParameters: RestZaakafhandelParameters
    ): ZaakafhandelParameters =
        zaakafhandelParameterService.readZaakafhandelParameters(
            restZaakafhandelParameters.zaaktype.uuid
        ).apply {
            id = restZaakafhandelParameters.id
            zaakTypeUUID = restZaakafhandelParameters.zaaktype.uuid
            zaaktypeOmschrijving = restZaakafhandelParameters.zaaktype.omschrijving
            caseDefinitionID = restZaakafhandelParameters.caseDefinition!!.key
            groepID = restZaakafhandelParameters.defaultGroepId
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                restZaakafhandelParameters.uiterlijkeEinddatumAfdoeningWaarschuwing
            nietOntvankelijkResultaattype = restZaakafhandelParameters.zaakNietOntvankelijkResultaattype!!.id
            intakeMail = restZaakafhandelParameters.intakeMail?.name
            afrondenMail = restZaakafhandelParameters.afrondenMail?.name
            // trim to make sure accidentally added whitespace is removed
            productaanvraagtype = restZaakafhandelParameters.productaanvraagtype?.trim()
            domein = restZaakafhandelParameters.domein
            gebruikersnaamMedewerker = restZaakafhandelParameters.defaultBehandelaarId
            einddatumGeplandWaarschuwing = restZaakafhandelParameters.einddatumGeplandWaarschuwing
            isSmartDocumentsIngeschakeld = restZaakafhandelParameters.smartDocuments.enabledForZaaktype
        }.also {
            it.setHumanTaskParametersCollection(
                humanTaskParametersConverter.convertRESTHumanTaskParameters(
                    restZaakafhandelParameters.humanTaskParameters
                )
            )
            it.setUserEventListenerParametersCollection(
                convertRESTUserEventListenerParameters(
                    restZaakafhandelParameters.userEventListenerParameters
                )
            )
            it.setZaakbeeindigParameters(
                convertRESTZaakbeeindigParameters(
                    restZaakafhandelParameters.zaakbeeindigParameters
                )
            )
            it.setMailtemplateKoppelingen(
                convertRESTmailtemplateKoppelingen(
                    restZaakafhandelParameters.mailtemplateKoppelingen
                )
            )
            it.setZaakAfzenders(
                convertRESTZaakAfzenders(restZaakafhandelParameters.zaakAfzenders)
            )
            restZaakafhandelParameters.betrokkeneKoppelingen.let { restBetrokkeneKoppelingen ->
                it.betrokkeneKoppelingen = restBetrokkeneKoppelingen.toBetrokkeneKoppelingen(it)
            }
            restZaakafhandelParameters.brpDoelbindingen.let { restBrpDoelbindingen ->
                it.brpDoelbindingen = restBrpDoelbindingen.toBrpDoelbindingen(it)
            }
        }
}
