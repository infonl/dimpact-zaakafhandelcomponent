/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import nl.info.zac.app.admin.converter.RestCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTHumanTaskParametersConverter
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter.convertRESTUserEventListenerParameters
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeCmmnExtension
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.app.admin.model.RestAutomaticEmailConfirmation
import nl.info.zac.app.admin.model.RestZaaktypeConfiguration
import nl.info.zac.app.admin.model.toAutomaticEmailConfirmation
import nl.info.zac.app.admin.model.toRestAutomaticEmailConfirmation
import nl.info.zac.app.admin.model.toRestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toRestBrpDoelbindingen
import nl.info.zac.app.admin.model.toRestSmartDocuments
import nl.info.zac.app.admin.model.toRestZaakAfzenders
import nl.info.zac.app.admin.model.toRestZaaktypeOverzicht
import nl.info.zac.app.admin.model.toZaakAfzenders
import nl.info.zac.app.admin.model.toZaaktypeBetrokkenParameters
import nl.info.zac.app.admin.model.toZaaktypeBrpParameters
import nl.info.zac.app.admin.model.toZaaktypeCompletionParametersList
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import java.time.ZonedDateTime

@AllOpen
@NoArgConstructor
@Suppress("LongParameterList")
class RestZaaktypeConfigurationConverter @Inject constructor(
    val caseDefinitionConverter: RestCaseDefinitionConverter,
    val zaakbeeindigParameterConverter: RestZaakbeeindigParameterConverter,
    val humanTaskParametersConverter: RESTHumanTaskParametersConverter,
    val ztcClientService: ZtcClientService,
    val resultaattypeReferenceService: ResultaattypeReferenceService,
    val zaaktypeConfigurationBeheerService: ZaaktypeConfigurationBeheerService,
    val smartDocumentsService: SmartDocumentsService,
    val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService,
) {
    @Suppress("LongMethod")
    fun toRestZaaktypeConfiguration(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        inclusiefRelaties: Boolean
    ): RestZaaktypeConfiguration {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid)
        val isBpmn = zaaktypeConfiguration.getProcessEngine() == ProcessEngine.BPMN
        val cmmnExtension = zaaktypeConfiguration.cmmnExtension.takeUnless { isBpmn } ?: ZaaktypeCmmnExtension()
        val restZaaktypeConfiguration = RestZaaktypeConfiguration(
            id = zaaktypeConfiguration.id,
            zaaktype = zaaktype.toRestZaaktypeOverzicht(),
            isZaakspecifiekAutoriseerbaar = zaakspecifiekeAutorisatieService.isZaakspecifiekAutoriseerbaar(zaaktype),
            defaultGroepId = zaaktypeConfiguration.groepID,
            defaultBehandelaarId = zaaktypeConfiguration.defaultBehandelaarId,
            einddatumGeplandWaarschuwing = zaaktypeConfiguration.einddatumGeplandWaarschuwing,
            uiterlijkeEinddatumAfdoeningWaarschuwing = zaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing,
            creatiedatum = zaaktypeConfiguration.creatiedatum,
            isValide = zaaktypeConfiguration.isValidForZaakCreation(),
            caseDefinition = zaaktypeConfiguration.processBinding
                ?.takeIf { it.processEngine == ProcessEngine.CMMN }
                ?.let { caseDefinitionConverter.convertToRestCaseDefinition(it.definitionKey, inclusiefRelaties) },
            intakeMail = cmmnExtension.intakeMail.takeUnless { isBpmn }?.let(ZaakafhandelparametersStatusMailOption::valueOf),
            afrondenMail = cmmnExtension.afrondenMail.takeUnless { isBpmn }
                ?.let(ZaakafhandelparametersStatusMailOption::valueOf),
            productaanvraagtype = zaaktypeConfiguration.productaanvraagtype,
            smartDocuments = zaaktypeConfiguration.toRestSmartDocuments(smartDocumentsService.isEnabled()),
            betrokkeneKoppelingen = zaaktypeConfiguration.getBetrokkeneParameters().toRestBetrokkeneKoppelingen(),
            brpDoelbindingen = zaaktypeConfiguration.getBrpParameters().toRestBrpDoelbindingen(),
            automaticEmailConfirmation = zaaktypeConfiguration.getAutomaticEmailConfirmation()
                ?.toRestAutomaticEmailConfirmation()
                ?: RestAutomaticEmailConfirmation(),
        )
        if (inclusiefRelaties) {
            restZaaktypeConfiguration.addRelatedData(zaaktypeConfiguration, cmmnExtension)
        }
        return restZaaktypeConfiguration
    }

    @Suppress("ThrowsCount")
    fun toZaaktypeConfiguration(restZaaktypeConfiguration: RestZaaktypeConfiguration): ZaaktypeConfiguration {
        val zaaktypeConfiguration =
            zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeConfiguration.zaaktype.uuid)
                ?: ZaaktypeConfiguration()
        return zaaktypeConfiguration.apply {
            id = restZaaktypeConfiguration.id
            zaaktypeUuid = restZaaktypeConfiguration.zaaktype.uuid
            zaaktypeOmschrijving = restZaaktypeConfiguration.zaaktype.omschrijving
                ?: throw NullPointerException("restZaakafhandelParameters.zaaktype.omschrijving is null")
            bindTo(
                ProcessEngine.CMMN,
                (
                    restZaaktypeConfiguration.caseDefinition
                        ?: throw NullPointerException("restZaakafhandelParameters.caseDefinition is null")
                    ).key
            )
            groepID = restZaaktypeConfiguration.defaultGroepId
                ?: throw NullPointerException("restZaakafhandelParameters.defaultGroepId is null")
            uiterlijkeEinddatumAfdoeningWaarschuwing = restZaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing
            nietOntvankelijkResultaattype = restZaaktypeConfiguration.zaakNietOntvankelijkResultaattype?.id
                ?: throw NullPointerException("restZaakafhandelParameters.zaakNietOntvankelijkResultaattype is null")
            productaanvraagtype = restZaaktypeConfiguration.productaanvraagtype?.trim()
            defaultBehandelaarId = restZaaktypeConfiguration.defaultBehandelaarId
            einddatumGeplandWaarschuwing = restZaaktypeConfiguration.einddatumGeplandWaarschuwing
            isSmartDocumentsEnabled = restZaaktypeConfiguration.smartDocuments.isEnabledForZaaktype
            creatiedatum = restZaaktypeConfiguration.creatiedatum ?: ZonedDateTime.now()
        }.also {
            it.getOrCreateCmmnExtension().apply {
                intakeMail = restZaaktypeConfiguration.intakeMail?.name
                afrondenMail = restZaaktypeConfiguration.afrondenMail?.name
                setHumanTaskParametersCollection(
                    humanTaskParametersConverter.convertRESTHumanTaskParameters(
                        restZaaktypeConfiguration.humanTaskParameters
                    )
                )
                setUserEventListenerParametersCollection(
                    convertRESTUserEventListenerParameters(restZaaktypeConfiguration.userEventListenerParameters)
                )
            }
            it.setZaakbeeindigParameters(
                restZaaktypeConfiguration.zaakbeeindigParameters.toZaaktypeCompletionParametersList()
            )
            it.setMailtemplateKoppelingen(
                restZaaktypeConfiguration.mailtemplateKoppelingen.map { koppeling ->
                    koppeling.toZaaktypeMailtemplateParameters()
                }
            )
            it.setZaakAfzenders(restZaaktypeConfiguration.zaakAfzenders.toZaakAfzenders())
            it.zaaktypeBetrokkeneParameters =
                restZaaktypeConfiguration.betrokkeneKoppelingen.toZaaktypeBetrokkenParameters(it)
            it.zaaktypeBrpParameters = restZaaktypeConfiguration.brpDoelbindingen.toZaaktypeBrpParameters(it)
            it.zaaktypeEmailParameters =
                restZaaktypeConfiguration.automaticEmailConfirmation.toAutomaticEmailConfirmation(it)
        }
    }

    private fun RestZaaktypeConfiguration.addRelatedData(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        cmmnExtension: ZaaktypeCmmnExtension
    ) {
        this.caseDefinition?.let { caseDefinition ->
            this.humanTaskParameters = humanTaskParametersConverter.convertHumanTaskParametersCollection(
                cmmnExtension.getHumanTaskParametersCollection(),
                caseDefinition.humanTaskDefinitions
            )
            this.userEventListenerParameters = RESTUserEventListenerParametersConverter
                .convertUserEventListenerParametersCollection(
                    cmmnExtension.getUserEventListenerParametersCollection(),
                    caseDefinition.userEventListenerDefinitions
                )
        }
        resultaattypeReferenceService.readNietOntvankelijkResultaattype(zaaktypeConfiguration)?.let {
            this.zaakNietOntvankelijkResultaattype = it.toRestResultaatType()
        }
        this.zaakbeeindigParameters = zaakbeeindigParameterConverter.convertZaakbeeindigParameters(
            zaaktypeConfiguration.getZaakbeeindigParameters()
        )
        this.mailtemplateKoppelingen = zaaktypeConfiguration.getMailtemplateKoppelingen().map {
            it.toRestMailtemplateKoppeling()
        }
        this.zaakAfzenders = zaaktypeConfiguration.getZaakAfzenders().toRestZaakAfzenders()
    }
}
