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
import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.ZaaktypeCmmnExtension
import nl.info.zac.admin.model.ZaaktypeEmailParameters
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.admin.model.ZaaktypeMailtemplateParameters
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.ZaaktypeZaakafzenderParameters
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
        previousZaaktypeConfiguration.processBinding?.let {
            newZaaktypeConfiguration.bindTo(it.processEngine, it.definitionKey)
        }
        previousZaaktypeConfiguration.cmmnExtension?.let {
            copyCmmnExtension(it, newZaaktypeConfiguration.getOrCreateCmmnExtension())
        }
    }

    private fun copyCmmnExtension(
        previousZaaktypeCmmnExtension: ZaaktypeCmmnExtension,
        newZaaktypeCmmnExtension: ZaaktypeCmmnExtension
    ) {
        newZaaktypeCmmnExtension.apply {
            intakeMail = previousZaaktypeCmmnExtension.intakeMail
            afrondenMail = previousZaaktypeCmmnExtension.afrondenMail
        }
        copyHumanTaskParameters(previousZaaktypeCmmnExtension, newZaaktypeCmmnExtension)
        copyUserEventListenerParameters(previousZaaktypeCmmnExtension, newZaaktypeCmmnExtension)
    }

    private fun copyHumanTaskParameters(
        previousZaaktypeCmmnExtension: ZaaktypeCmmnExtension,
        newZaaktypeCmmnExtension: ZaaktypeCmmnExtension
    ) = previousZaaktypeCmmnExtension.getHumanTaskParametersCollection().map {
        ZaaktypeCmmnHumantaskParameters().apply {
            doorlooptijd = it.doorlooptijd
            isActief = it.isActief
            setFormulierDefinitieID(it.getFormulierDefinitieID())
            planItemDefinitionID = it.planItemDefinitionID
            groepID = it.groepID
            setReferentieTabellen(it.getReferentieTabellen())
        }
    }.toSet().let(newZaaktypeCmmnExtension::setHumanTaskParametersCollection)

    private fun copyUserEventListenerParameters(
        previousZaaktypeCmmnExtension: ZaaktypeCmmnExtension,
        newZaaktypeCmmnExtension: ZaaktypeCmmnExtension
    ) = previousZaaktypeCmmnExtension.getUserEventListenerParametersCollection().map {
        ZaaktypeCmmnUsereventlistenerParameters().apply {
            planItemDefinitionID = it.planItemDefinitionID
            toelichting = it.toelichting
        }
    }.toSet().let(newZaaktypeCmmnExtension::setUserEventListenerParametersCollection)

    private fun copyMailtemplateKoppelingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) = previousZaaktypeConfiguration.getMailtemplateKoppelingen().map {
        ZaaktypeMailtemplateParameters().apply {
            mailTemplate = it.mailTemplate
            zaaktypeConfiguration = newZaaktypeConfiguration
        }
    }.let(newZaaktypeConfiguration::setMailtemplateKoppelingen)

    private fun copyZaakAfzenders(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) = previousZaaktypeConfiguration.getZaakAfzenders().map {
        ZaaktypeZaakafzenderParameters().apply {
            isDefaultMail = it.isDefaultMail
            mail = it.mail
            replyTo = it.replyTo
            zaaktypeConfiguration = newZaaktypeConfiguration
        }
    }.let(newZaaktypeConfiguration::setZaakAfzenders)

    private fun copyAutomaticEmailConfirmation(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) {
        newZaaktypeConfiguration.zaaktypeEmailParameters =
            previousZaaktypeConfiguration.zaaktypeEmailParameters?.let { previousZaaktypeEmailParameters ->
                ZaaktypeEmailParameters().apply {
                    zaaktypeConfiguration = newZaaktypeConfiguration
                    isEnabled = previousZaaktypeEmailParameters.isEnabled
                    templateName = previousZaaktypeEmailParameters.templateName
                    emailSender = previousZaaktypeEmailParameters.emailSender
                    emailReply = previousZaaktypeEmailParameters.emailReply
                }
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
            isSmartDocumentsEnabled = previousZaaktypeConfiguration.isSmartDocumentsEnabled
            creatiedatum = ZonedDateTime.now()
            einddatumGeplandWaarschuwing = previousZaaktypeConfiguration.einddatumGeplandWaarschuwing.takeIf {
                newZaaktype.isServicenormAvailable()
            }
            uiterlijkeEinddatumAfdoeningWaarschuwing = previousZaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing
        }
        copyBetrokkeneKoppelingen(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        copyMailtemplateKoppelingen(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        copyZaakAfzenders(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        copyAutomaticEmailConfirmation(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        copyBrpDoelbindingen(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        mapZaakbeeindigGegevens(previousZaaktypeConfiguration, newZaaktypeConfiguration, newZaaktype)
    }

    private fun copyBetrokkeneKoppelingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) {
        newZaaktypeConfiguration.zaaktypeBetrokkeneParameters = ZaaktypeBetrokkeneParameters().apply {
            zaaktypeConfiguration = newZaaktypeConfiguration
            isBrpKoppelenEnabled = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.isBrpKoppelenEnabled
            isKvkKoppelenEnabled = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.isKvkKoppelenEnabled
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
