/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTHumanTaskParametersConverter
import net.atos.zac.app.admin.converter.RESTMailtemplateKoppelingConverter
import net.atos.zac.app.admin.converter.RESTMailtemplateKoppelingConverter.convertRESTmailtemplateKoppelingen
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter.convertRESTUserEventListenerParameters
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter.convertRESTZaakbeeindigParameters
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.model.RestAutomaticEmailConfirmation
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
    val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService,
    val smartDocumentsService: SmartDocumentsService,
) {
    @Suppress("LongMethod")
    fun toRestZaaktypeCmmnConfiguration(
        zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        inclusiefRelaties: Boolean
    ): RestZaakafhandelParameters {
        val restZaakafhandelParameters = RestZaakafhandelParameters(
            id = zaaktypeCmmnConfiguration.id,
            zaaktype = ztcClientService.readZaaktype(
                zaaktypeCmmnConfiguration.zaakTypeUUID!!
            ).toRestZaaktypeOverzicht(),
            defaultGroepId = zaaktypeCmmnConfiguration.groepID,
            defaultBehandelaarId = zaaktypeCmmnConfiguration.gebruikersnaamMedewerker,
            einddatumGeplandWaarschuwing = zaaktypeCmmnConfiguration.einddatumGeplandWaarschuwing,
            uiterlijkeEinddatumAfdoeningWaarschuwing = zaaktypeCmmnConfiguration
                .uiterlijkeEinddatumAfdoeningWaarschuwing,
            creatiedatum = zaaktypeCmmnConfiguration.creatiedatum,
            valide = zaaktypeCmmnConfiguration.isValide(),
            caseDefinition = zaaktypeCmmnConfiguration.caseDefinitionID?.let {
                caseDefinitionConverter.convertToRESTCaseDefinition(it, inclusiefRelaties)
            },
            intakeMail = zaaktypeCmmnConfiguration.intakeMail?.let {
                ZaakafhandelparametersStatusMailOption.valueOf(
                    it
                )
            },
            afrondenMail = zaaktypeCmmnConfiguration.afrondenMail?.let {
                ZaakafhandelparametersStatusMailOption.valueOf(
                    it
                )
            },
            productaanvraagtype = zaaktypeCmmnConfiguration.productaanvraagtype,
            domein = zaaktypeCmmnConfiguration.domein,
            smartDocuments = RestSmartDocuments(
                enabledGlobally = smartDocumentsService.isEnabled(),
                enabledForZaaktype = zaaktypeCmmnConfiguration.smartDocumentsIngeschakeld
            ),
            betrokkeneKoppelingen = zaaktypeCmmnConfiguration.getBetrokkeneParameters()
                .toRestBetrokkeneKoppelingen(),
            brpDoelbindingen = zaaktypeCmmnConfiguration.getBrpDoelbindingen()
                .toRestBrpDoelbindingen(),
            automaticEmailConfirmation = zaaktypeCmmnConfiguration.getAutomaticEmailConfirmation()
                ?.toRestAutomaticEmailConfirmation()
                ?: RestAutomaticEmailConfirmation(),
        )
        if (inclusiefRelaties) {
            restZaakafhandelParameters.addRelatedData(zaaktypeCmmnConfiguration)
        }
        return restZaakafhandelParameters
    }

    private fun RestZaakafhandelParameters.addRelatedData(zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration) {
        this.caseDefinition?.let { caseDefinition ->
            this.humanTaskParameters =
                humanTaskParametersConverter.convertHumanTaskParametersCollection(
                    zaaktypeCmmnConfiguration.getHumanTaskParametersCollection(),
                    caseDefinition.humanTaskDefinitions
                )
            this.userEventListenerParameters = RESTUserEventListenerParametersConverter
                .convertUserEventListenerParametersCollection(
                    zaaktypeCmmnConfiguration.getUserEventListenerParametersCollection(),
                    caseDefinition.userEventListenerDefinitions
                )
        }
        zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype?.let {
            ztcClientService.readResultaattype(it).let { resultaatType ->
                this.zaakNietOntvankelijkResultaattype =
                    resultaatType.toRestResultaatType()
            }
        }
        this.zaakbeeindigParameters =
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(
                zaaktypeCmmnConfiguration.getZaakbeeindigParameters()
            )
        this.mailtemplateKoppelingen = RESTMailtemplateKoppelingConverter.convert(
            zaaktypeCmmnConfiguration.getMailtemplateKoppelingen()
        )
        this.zaakAfzenders = zaaktypeCmmnConfiguration.getZaakAfzenders().toRestZaakAfzenders()
    }

    @Suppress("ThrowsCount")
    fun toZaaktypeCmmnConfiguration(
        restZaakafhandelParameters: RestZaakafhandelParameters
    ): ZaaktypeCmmnConfiguration {
        val zaaktypeCmmnConfiguration = restZaakafhandelParameters.zaaktype.uuid?.let { uuid ->
            zaaktypeCmmnConfigurationBeheerService.fetchZaaktypeCmmnConfiguration(uuid)
        } ?: throw NullPointerException("restZaakafhandelParameters.zaaktype.uuid is null")

        return zaaktypeCmmnConfiguration.apply {
            id = restZaakafhandelParameters.id
            zaakTypeUUID = restZaakafhandelParameters.zaaktype.uuid
            zaaktypeOmschrijving = restZaakafhandelParameters.zaaktype.omschrijving
                ?: throw NullPointerException("restZaakafhandelParameters.zaaktype.omschrijving is null")
            caseDefinitionID = (
                restZaakafhandelParameters.caseDefinition
                    ?: throw NullPointerException("restZaakafhandelParameters.caseDefinition is null")
                ).key

            groepID = restZaakafhandelParameters.defaultGroepId
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                restZaakafhandelParameters.uiterlijkeEinddatumAfdoeningWaarschuwing
            nietOntvankelijkResultaattype = restZaakafhandelParameters.zaakNietOntvankelijkResultaattype?.id
                ?: throw NullPointerException("restZaakafhandelParameters.zaakNietOntvankelijkResultaattype is null")
            intakeMail = restZaakafhandelParameters.intakeMail?.name
            afrondenMail = restZaakafhandelParameters.afrondenMail?.name
            productaanvraagtype = restZaakafhandelParameters.productaanvraagtype?.trim()
            domein = restZaakafhandelParameters.domein
            gebruikersnaamMedewerker = restZaakafhandelParameters.defaultBehandelaarId
            einddatumGeplandWaarschuwing = restZaakafhandelParameters.einddatumGeplandWaarschuwing
            smartDocumentsIngeschakeld = restZaakafhandelParameters.smartDocuments.enabledForZaaktype
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
            it.zaaktypeCmmnBetrokkeneParameters =
                restZaakafhandelParameters.betrokkeneKoppelingen.toBetrokkeneKoppelingen(it)
            it.zaaktypeCmmnBrpParameters =
                restZaakafhandelParameters.brpDoelbindingen.toBrpDoelbindingen(it)
            it.zaaktypeCmmnEmailParameters =
                restZaakafhandelParameters.automaticEmailConfirmation.toAutomaticEmailConfirmation(it)
        }
    }
}
