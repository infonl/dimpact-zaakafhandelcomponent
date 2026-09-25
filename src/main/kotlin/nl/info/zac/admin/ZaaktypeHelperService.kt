/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ZaaktypeBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnEmailParameters
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.admin.model.ZaaktypeCmmnMailtemplateParameters
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class ZaaktypeHelperService @Inject constructor(
    private val ztcClientService: ZtcClientService,
) {
    fun copyConfigurationData(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        copySharedConfigurationData(previousZaaktypeConfiguration, newZaaktypeConfiguration, newZaaktype)
        when {
            previousZaaktypeConfiguration is ZaaktypeCmmnConfiguration &&
                newZaaktypeConfiguration is ZaaktypeCmmnConfiguration ->
                copyCmmnConfigurationData(previousZaaktypeConfiguration, newZaaktypeConfiguration, newZaaktype)
            previousZaaktypeConfiguration is ZaaktypeBpmnConfiguration &&
                newZaaktypeConfiguration is ZaaktypeBpmnConfiguration ->
                copyBpmnConfigurationData(previousZaaktypeConfiguration, newZaaktypeConfiguration)
            else -> throw IllegalArgumentException(
                "Cannot copy a ${previousZaaktypeConfiguration.getConfigurationType()} zaaktype configuration " +
                    "onto a ${newZaaktypeConfiguration.getConfigurationType()} zaaktype configuration"
            )
        }
    }

    private fun copyCmmnConfigurationData(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktype: ZaakType
    ) {
        newZaaktypeCmmnConfiguration.apply {
            caseDefinitionID = previousZaaktypeCmmnConfiguration.caseDefinitionID
            einddatumGeplandWaarschuwing = previousZaaktypeCmmnConfiguration.einddatumGeplandWaarschuwing.takeIf {
                newZaaktype.isServicenormAvailable()
            }
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                previousZaaktypeCmmnConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing
            intakeMail = previousZaaktypeCmmnConfiguration.intakeMail
            afrondenMail = previousZaaktypeCmmnConfiguration.afrondenMail
        }
        copyHumanTaskParameters(previousZaaktypeCmmnConfiguration, newZaaktypeCmmnConfiguration)
        copyUserEventListenerParameters(previousZaaktypeCmmnConfiguration, newZaaktypeCmmnConfiguration)
        copyMailtemplateKoppelingen(previousZaaktypeCmmnConfiguration, newZaaktypeCmmnConfiguration)
        copyZaakAfzenders(previousZaaktypeCmmnConfiguration, newZaaktypeCmmnConfiguration)
        copyAutomaticEmailConfirmation(previousZaaktypeCmmnConfiguration, newZaaktypeCmmnConfiguration)
    }

    private fun copyBpmnConfigurationData(
        previousZaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration,
        newZaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration
    ) {
        newZaaktypeBpmnConfiguration.bpmnProcessDefinitionKey =
            previousZaaktypeBpmnConfiguration.bpmnProcessDefinitionKey
    }

    private fun copyHumanTaskParameters(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getHumanTaskParametersCollection().map {
        ZaaktypeCmmnHumantaskParameters().apply {
            doorlooptijd = it.doorlooptijd
            actief = it.actief
            setFormulierDefinitieID(it.getFormulierDefinitieID())
            planItemDefinitionID = it.planItemDefinitionID
            groepID = it.groepID
            setReferentieTabellen(it.getReferentieTabellen())
        }
    }.toSet().let(newZaaktypeCmmnConfiguration::setHumanTaskParametersCollection)

    private fun copyUserEventListenerParameters(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getUserEventListenerParametersCollection().map {
        ZaaktypeCmmnUsereventlistenerParameters().apply {
            planItemDefinitionID = it.planItemDefinitionID
            toelichting = it.toelichting
        }
    }.toSet().let(newZaaktypeCmmnConfiguration::setUserEventListenerParametersCollection)

    private fun copyMailtemplateKoppelingen(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getMailtemplateKoppelingen().map {
        ZaaktypeCmmnMailtemplateParameters().apply {
            mailTemplate = it.mailTemplate
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
        }
    }.let(newZaaktypeCmmnConfiguration::setMailtemplateKoppelingen)

    private fun copyZaakAfzenders(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getZaakAfzenders().map {
        ZaaktypeCmmnZaakafzenderParameters().apply {
            defaultMail = it.defaultMail
            mail = it.mail
            replyTo = it.replyTo
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
        }
    }.let(newZaaktypeCmmnConfiguration::setZaakAfzenders)

    private fun copyAutomaticEmailConfirmation(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) {
        newZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters = ZaaktypeCmmnEmailParameters().apply {
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
            enabled = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.enabled ?: false
            templateName = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName
            emailSender = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailSender
            emailReply = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailReply
        }
    }

    private fun copySharedConfigurationData(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        newZaaktypeConfiguration.apply {
            groepID = previousZaaktypeConfiguration.groepID
            defaultBehandelaarId = previousZaaktypeConfiguration.defaultBehandelaarId
            productaanvraagtype = previousZaaktypeConfiguration.productaanvraagtype
            smartDocumentsEnabled = previousZaaktypeConfiguration.smartDocumentsEnabled
            creatiedatum = ZonedDateTime.now()
        }
        copyBetrokkeneKoppelingen(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        copyBrpDoelbindingen(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        mapZaakbeeindigGegevens(previousZaaktypeConfiguration, newZaaktypeConfiguration, newZaaktype)
    }

    private fun copyBetrokkeneKoppelingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) {
        newZaaktypeConfiguration.zaaktypeBetrokkeneParameters = ZaaktypeBetrokkeneParameters().apply {
            zaaktypeConfiguration = newZaaktypeConfiguration
            brpKoppelen = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.brpKoppelen
            kvkKoppelen = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.kvkKoppelen
        }
    }

    private fun copyBrpDoelbindingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) {
        newZaaktypeConfiguration.zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
            zaaktypeConfiguration = newZaaktypeConfiguration
            zoekWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.zoekWaarde
            raadpleegWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.raadpleegWaarde
            verwerkingregisterWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.verwerkingregisterWaarde
        }
    }

    fun updateZaakbeeindigGegevens(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) = mapZaakbeeindigGegevens(zaaktypeConfiguration, zaaktypeConfiguration, newZaaktype)

    private fun mapPreviousResultaattypeToNewResultaattype(
        previousResultaattypeUUID: UUID,
        newResultaattypen: List<ResultaatType>,
    ): UUID? =
        ztcClientService.readResultaattype(previousResultaattypeUUID).let { previousResultaattype ->
            newResultaattypen.firstOrNull { it.omschrijving == previousResultaattype.omschrijving }
        }
            ?.url
            ?.extractUuid()

    /**
     * Source and destination may be the same instance; everything is read into local variables before the first write.
     */
    fun mapZaakbeeindigGegevens(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        val nietOntvankelijkResultaattype = previousZaaktypeConfiguration.nietOntvankelijkResultaattype?.let {
            mapPreviousResultaattypeToNewResultaattype(it, newResultaattypen)
        }
        val zaakbeeindigParametersCollection = previousZaaktypeConfiguration.getZaakbeeindigParameters()
            .mapNotNull { zaakbeeindigParameter ->
                zaakbeeindigParameter.resultaattype
                    .let { mapPreviousResultaattypeToNewResultaattype(it, newResultaattypen) }
                    ?.let {
                        ZaaktypeCompletionParameters().apply {
                            zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                            resultaattype = it
                        }
                    }
            }.toMutableSet()
        newZaaktypeConfiguration.nietOntvankelijkResultaattype = nietOntvankelijkResultaattype
        newZaaktypeConfiguration.setZaakbeeindigParameters(zaakbeeindigParametersCollection)
    }
}
