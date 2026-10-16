/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.app.admin.converter.RESTHumanTaskParametersConverter
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter
import net.atos.zac.app.admin.converter.RESTUserEventListenerParametersConverter.convertRESTUserEventListenerParameters
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
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
    val processDefinitionConverter: RestProcessDefinitionConverter,
    val zaakbeeindigParameterConverter: RestZaakbeeindigParameterConverter,
    val humanTaskParametersConverter: RESTHumanTaskParametersConverter,
    val ztcClientService: ZtcClientService,
    val resultaattypeReferenceService: ResultaattypeReferenceService,
    val zaaktypeConfigurationBeheerService: ZaaktypeConfigurationBeheerService,
    val smartDocumentsService: SmartDocumentsService,
    val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService,
) {
    fun toRestZaaktypeConfiguration(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        inclusiefRelaties: Boolean,
        zaaktype: ZaakType? = null
    ): RestZaaktypeConfiguration {
        val resolvedZaaktype = zaaktype ?: ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid)
        val cmmnExtension = zaaktypeConfiguration.cmmnExtension
        val restZaaktypeConfiguration = RestZaaktypeConfiguration(
            id = zaaktypeConfiguration.id,
            zaaktype = resolvedZaaktype.toRestZaaktypeOverzicht(),
            processDefinition = zaaktypeConfiguration.processBinding?.let {
                processDefinitionConverter.toRestProcessDefinition(it, inclusiefRelaties)
            },
            isZaakspecifiekAutoriseerbaar = zaakspecifiekeAutorisatieService.isZaakspecifiekAutoriseerbaar(
                resolvedZaaktype
            ),
            defaultGroepId = zaaktypeConfiguration.groepID,
            defaultBehandelaarId = zaaktypeConfiguration.defaultBehandelaarId,
            einddatumGeplandWaarschuwing = zaaktypeConfiguration.einddatumGeplandWaarschuwing,
            uiterlijkeEinddatumAfdoeningWaarschuwing = zaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing,
            creatiedatum = zaaktypeConfiguration.creatiedatum,
            isValide = zaaktypeConfiguration.isValidForZaakCreation(),
            productaanvraagtype = zaaktypeConfiguration.productaanvraagtype,
            intakeMail = cmmnExtension?.intakeMail?.let(ZaakafhandelparametersStatusMailOption::valueOf),
            afrondenMail = cmmnExtension?.afrondenMail?.let(ZaakafhandelparametersStatusMailOption::valueOf),
            automaticEmailConfirmation = zaaktypeConfiguration.getAutomaticEmailConfirmation()
                ?.toRestAutomaticEmailConfirmation()
                ?: RestAutomaticEmailConfirmation(),
            smartDocuments = zaaktypeConfiguration.toRestSmartDocuments(smartDocumentsService.isEnabled()),
            betrokkeneKoppelingen = zaaktypeConfiguration.getBetrokkeneParameters().toRestBetrokkeneKoppelingen(),
            brpDoelbindingen = zaaktypeConfiguration.getBrpParameters().toRestBrpDoelbindingen(),
        )
        if (inclusiefRelaties) {
            restZaaktypeConfiguration.addRelatedData(zaaktypeConfiguration, cmmnExtension)
        }
        return restZaaktypeConfiguration
    }

    /**
     * Converts the REST configuration to the stored configuration of its zaaktype version, and updates that stored
     * configuration when there is one. Only a configuration that is bound to a CMMN case definition stores the plan
     * item settings, as only such a configuration has a CMMN extension.
     */
    fun toZaaktypeConfiguration(restZaaktypeConfiguration: RestZaaktypeConfiguration): ZaaktypeConfiguration {
        val zaaktypeConfiguration =
            zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeConfiguration.zaaktype.uuid)
                ?: ZaaktypeConfiguration()
        return zaaktypeConfiguration.apply {
            id = restZaaktypeConfiguration.id
            zaaktypeUuid = restZaaktypeConfiguration.zaaktype.uuid
            zaaktypeOmschrijving = restZaaktypeConfiguration.zaaktype.omschrijving
                ?: throw NullPointerException("restZaakafhandelParameters.zaaktype.omschrijving is null")
            groepID = restZaaktypeConfiguration.defaultGroepId
                ?: throw NullPointerException("restZaakafhandelParameters.defaultGroepId is null")
            uiterlijkeEinddatumAfdoeningWaarschuwing = restZaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing
            nietOntvankelijkResultaattypeOmschrijving = restZaaktypeConfiguration.zaakNietOntvankelijkResultaattype
                ?.let { resultaattypeReferenceService.readOmschrijving(it.id) }
            productaanvraagtype = restZaaktypeConfiguration.productaanvraagtype?.trim()
            defaultBehandelaarId = restZaaktypeConfiguration.defaultBehandelaarId
            einddatumGeplandWaarschuwing = restZaaktypeConfiguration.einddatumGeplandWaarschuwing
            isSmartDocumentsEnabled = restZaaktypeConfiguration.smartDocuments.isEnabledForZaaktype
            creatiedatum = restZaaktypeConfiguration.creatiedatum ?: ZonedDateTime.now()
            zaaktypeEmailParameters = restZaaktypeConfiguration.automaticEmailConfirmation
                .toAutomaticEmailConfirmation(this)
        }.also {
            it.applyProcessDefinition(restZaaktypeConfiguration)
            it.setZaakbeeindigParameters(
                zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(
                    restZaaktypeConfiguration.zaakbeeindigParameters
                )
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
        }
    }

    private fun ZaaktypeConfiguration.applyProcessDefinition(restZaaktypeConfiguration: RestZaaktypeConfiguration) {
        val processDefinition = restZaaktypeConfiguration.processDefinition
            ?: throw NullPointerException("restZaakafhandelParameters.processDefinition is null")
        bindTo(processDefinition.processEngine, processDefinition.key)
        cmmnExtension?.apply {
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
    }

    private fun RestZaaktypeConfiguration.addRelatedData(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        cmmnExtension: ZaaktypeCmmnExtension?
    ) {
        processDefinition?.let {
            humanTaskParameters = humanTaskParametersConverter.convertHumanTaskParametersCollection(
                cmmnExtension?.getHumanTaskParametersCollection().orEmpty(),
                it.humanTaskDefinitions
            )
            userEventListenerParameters = RESTUserEventListenerParametersConverter
                .convertUserEventListenerParametersCollection(
                    cmmnExtension?.getUserEventListenerParametersCollection().orEmpty(),
                    it.userEventListenerDefinitions
                )
        }
        resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeConfiguration)?.let {
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
