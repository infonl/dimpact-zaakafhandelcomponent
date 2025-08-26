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
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter.convertRESTZaakbeeindigParameters
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.app.admin.model.RestAutomaticEmailConfirmation
import nl.info.zac.app.admin.model.RestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.RestBrpDoelbindingen
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.admin.model.RestZaakafhandelParameters
import nl.info.zac.app.admin.model.toAutomaticEmailConfirmation
import nl.info.zac.app.admin.model.toBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toBrpDoelbindingen
import nl.info.zac.app.admin.model.toRestAutomaticEmailConfirmation
import nl.info.zac.app.admin.model.toRestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toRestBrpDoelbindingen
import nl.info.zac.app.admin.model.toRestZaakAfzenders
import nl.info.zac.app.admin.model.toRestZaaktypeOverzicht
import nl.info.zac.app.admin.model.toZaakAfzenders
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
            zaaktype = ztcClientService.readZaaktype(zaakafhandelParameters.zaakTypeUUID).toRestZaaktypeOverzicht(),
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
            intakeMail = zaakafhandelParameters.intakeMail?.let { ZaakafhandelparametersStatusMailOption.valueOf(it) },
            afrondenMail = zaakafhandelParameters.afrondenMail?.let {
                ZaakafhandelparametersStatusMailOption.valueOf(
                    it
                )
            },
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
            automaticEmailConfirmation = zaakafhandelParameters.automaticEmailConfirmation
                ?.toRestAutomaticEmailConfirmation()
                ?: RestAutomaticEmailConfirmation(),
        )
        if (inclusiefRelaties) {
            restZaakafhandelParameters.addRelations(zaakafhandelParameters)
        }
        return restZaakafhandelParameters
    }

    private fun RestZaakafhandelParameters.addRelations(zaakafhandelParameters: ZaakafhandelParameters) {
        this.caseDefinition?.let { caseDefinition ->
            this.humanTaskParameters =
                humanTaskParametersConverter.convertHumanTaskParametersCollection(
                    zaakafhandelParameters.humanTaskParametersCollection,
                    caseDefinition.humanTaskDefinitions
                )
            this.userEventListenerParameters = RESTUserEventListenerParametersConverter
                .convertUserEventListenerParametersCollection(
                    zaakafhandelParameters.userEventListenerParametersCollection,
                    caseDefinition.userEventListenerDefinitions
                )
        }
        zaakafhandelParameters.nietOntvankelijkResultaattype?.let {
            ztcClientService.readResultaattype(it).let { resultaatType ->
                this.zaakNietOntvankelijkResultaattype =
                    resultaatType.toRestResultaatType()
            }
        }
        this.zaakbeeindigParameters =
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(
                zaakafhandelParameters.zaakbeeindigParameters
            )
        this.mailtemplateKoppelingen = RESTMailtemplateKoppelingConverter.convert(
            zaakafhandelParameters.mailtemplateKoppelingen
        )
        this.zaakAfzenders = zaakafhandelParameters.zaakAfzenders.toRestZaakAfzenders()
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
            it.setZaakAfzenders(restZaakafhandelParameters.zaakAfzenders.toZaakAfzenders())
            it.betrokkeneKoppelingen = restZaakafhandelParameters.betrokkeneKoppelingen.toBetrokkeneKoppelingen(it)
            it.brpDoelbindingen = restZaakafhandelParameters.brpDoelbindingen.toBrpDoelbindingen(it)
            it.automaticEmailConfirmation = restZaakafhandelParameters.automaticEmailConfirmation
                .toAutomaticEmailConfirmation(it)
        }
}
