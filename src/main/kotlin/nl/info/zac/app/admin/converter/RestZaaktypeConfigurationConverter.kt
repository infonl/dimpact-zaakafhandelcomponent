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
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.ZtcClientService.Companion.ZAAK_GEAUTORISEERD_EIGENSCHAP_NAAM
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
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
import java.time.ZonedDateTime

@AllOpen
@NoArgConstructor
@Suppress("LongParameterList")
class RestZaaktypeConfigurationConverter @Inject constructor(
    val caseDefinitionConverter: RESTCaseDefinitionConverter,
    val zaakbeeindigParameterConverter: RestZaakbeeindigParameterConverter,
    val humanTaskParametersConverter: RESTHumanTaskParametersConverter,
    val ztcClientService: ZtcClientService,
    val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService,
    val smartDocumentsService: SmartDocumentsService,
) {
    @Suppress("LongMethod")
    fun toRestZaaktypeConfiguration(
        zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        inclusiefRelaties: Boolean
    ): RestZaaktypeConfiguration {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeCmmnConfiguration.zaaktypeUuid)
        val restZaaktypeConfiguration = RestZaaktypeConfiguration(
            id = zaaktypeCmmnConfiguration.id,
            zaaktype = zaaktype.toRestZaaktypeOverzicht(),
            zaakspecifiekAutoriseerbaar = zaaktype.isZaakspecifiekAutoriseerbaar(),
            defaultGroepId = zaaktypeCmmnConfiguration.groepID,
            defaultBehandelaarId = zaaktypeCmmnConfiguration.defaultBehandelaarId,
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
            smartDocuments = zaaktypeCmmnConfiguration.toRestSmartDocuments(smartDocumentsService.isEnabled()),
            betrokkeneKoppelingen = zaaktypeCmmnConfiguration.getBetrokkeneParameters()
                .toRestBetrokkeneKoppelingen(),
            brpDoelbindingen = zaaktypeCmmnConfiguration.getBrpParameters()
                .toRestBrpDoelbindingen(),
            automaticEmailConfirmation = zaaktypeCmmnConfiguration.getAutomaticEmailConfirmation()
                ?.toRestAutomaticEmailConfirmation()
                ?: RestAutomaticEmailConfirmation(),
        )
        if (inclusiefRelaties) {
            restZaaktypeConfiguration.addRelatedData(zaaktypeCmmnConfiguration)
        }
        return restZaaktypeConfiguration
    }

    @Suppress("ThrowsCount")
    fun toZaaktypeCmmnConfiguration(
        restZaaktypeConfiguration: RestZaaktypeConfiguration
    ): ZaaktypeCmmnConfiguration =
        zaaktypeCmmnConfigurationBeheerService.fetchZaaktypeCmmnConfiguration(
            restZaaktypeConfiguration.zaaktype.uuid
        ).apply {
            id = restZaaktypeConfiguration.id
            zaaktypeUuid = restZaaktypeConfiguration.zaaktype.uuid
            zaaktypeOmschrijving = restZaaktypeConfiguration.zaaktype.omschrijving
                ?: throw NullPointerException("restZaakafhandelParameters.zaaktype.omschrijving is null")
            caseDefinitionID = (
                restZaaktypeConfiguration.caseDefinition
                    ?: throw NullPointerException("restZaakafhandelParameters.caseDefinition is null")
                ).key
            groepID = restZaaktypeConfiguration.defaultGroepId
                ?: throw NullPointerException("restZaakafhandelParameters.defaultGroepId is null")
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                restZaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing
            nietOntvankelijkResultaattype = restZaaktypeConfiguration.zaakNietOntvankelijkResultaattype?.id
                ?: throw NullPointerException("restZaakafhandelParameters.zaakNietOntvankelijkResultaattype is null")
            intakeMail = restZaaktypeConfiguration.intakeMail?.name
            afrondenMail = restZaaktypeConfiguration.afrondenMail?.name
            productaanvraagtype = restZaaktypeConfiguration.productaanvraagtype?.trim()
            defaultBehandelaarId = restZaaktypeConfiguration.defaultBehandelaarId
            einddatumGeplandWaarschuwing = restZaaktypeConfiguration.einddatumGeplandWaarschuwing
            smartDocumentsEnabled = restZaaktypeConfiguration.smartDocuments.enabledForZaaktype
            creatiedatum = restZaaktypeConfiguration.creatiedatum ?: ZonedDateTime.now()
        }.also {
            it.setHumanTaskParametersCollection(
                humanTaskParametersConverter.convertRESTHumanTaskParameters(
                    restZaaktypeConfiguration.humanTaskParameters
                )
            )
            it.setUserEventListenerParametersCollection(
                convertRESTUserEventListenerParameters(
                    restZaaktypeConfiguration.userEventListenerParameters
                )
            )
            it.setZaakbeeindigParameters(
                restZaaktypeConfiguration.zaakbeeindigParameters.toZaaktypeCompletionParametersList()
            )
            it.setMailtemplateKoppelingen(
                convertRESTmailtemplateKoppelingen(
                    restZaaktypeConfiguration.mailtemplateKoppelingen
                )
            )
            it.setZaakAfzenders(restZaaktypeConfiguration.zaakAfzenders.toZaakAfzenders())
            it.zaaktypeBetrokkeneParameters =
                restZaaktypeConfiguration.betrokkeneKoppelingen.toZaaktypeBetrokkenParameters(it)
            it.zaaktypeBrpParameters =
                restZaaktypeConfiguration.brpDoelbindingen.toZaaktypeBrpParameters(it)
            it.zaaktypeCmmnEmailParameters =
                restZaaktypeConfiguration.automaticEmailConfirmation.toAutomaticEmailConfirmation(it)
        }

    @Suppress("LongMethod")
    fun toRestZaaktypeConfiguration(
        zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration
    ): RestZaaktypeConfiguration {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeBpmnConfiguration.zaaktypeUuid)
        val restZaaktypeConfiguration = RestZaaktypeConfiguration(
            id = zaaktypeBpmnConfiguration.id,
            zaaktype = zaaktype.toRestZaaktypeOverzicht(),
            zaakspecifiekAutoriseerbaar = zaaktype.isZaakspecifiekAutoriseerbaar(),
            defaultGroepId = zaaktypeBpmnConfiguration.groepID,
            creatiedatum = zaaktypeBpmnConfiguration.creatiedatum,
            productaanvraagtype = zaaktypeBpmnConfiguration.productaanvraagtype,
            smartDocuments = zaaktypeBpmnConfiguration.toRestSmartDocuments(smartDocumentsService.isEnabled()),
            betrokkeneKoppelingen = zaaktypeBpmnConfiguration.getBetrokkeneParameters()
                .toRestBetrokkeneKoppelingen(),
            brpDoelbindingen = zaaktypeBpmnConfiguration.getBrpParameters()
                .toRestBrpDoelbindingen(),
            zaakNietOntvankelijkResultaattype = zaaktypeBpmnConfiguration.nietOntvankelijkResultaattype?.let {
                ztcClientService.readResultaattype(it).toRestResultaatType()
            },
            zaakbeeindigParameters = zaakbeeindigParameterConverter.convertZaakbeeindigParameters(
                zaaktypeBpmnConfiguration.getZaakbeeindigParameters()
            )
        )
        return restZaaktypeConfiguration
    }

    private fun ZaakType.isZaakspecifiekAutoriseerbaar() =
        ztcClientService.findEigenschap(
            zaaktype = getUrl(),
            eigenschap = ZAAK_GEAUTORISEERD_EIGENSCHAP_NAAM
        ) != null

    private fun RestZaaktypeConfiguration.addRelatedData(zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration) {
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
}
