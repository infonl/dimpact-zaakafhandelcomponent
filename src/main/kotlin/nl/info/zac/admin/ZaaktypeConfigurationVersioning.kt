/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ZaaktypeBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.ZaaktypeCmmnExtension
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.ZaaktypeEmailParameters
import nl.info.zac.admin.model.ZaaktypeMailtemplateParameters
import nl.info.zac.admin.model.ZaaktypeZaakafzenderParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ZaaktypeConfigurationVersioning @Inject constructor(
    private val ztcClientService: ZtcClientService,
) {
    /**
     * Returns a new, unsaved configuration for [newZaaktype] with the settings of [previous]. A resultaattype
     * reference whose omschrijving [newZaaktype] lacks is dropped.
     */
    fun createNextVersion(previous: ZaaktypeConfiguration, newZaaktype: ZaakType): ZaaktypeConfiguration {
        val newResultaattypeOmschrijvingen = newZaaktype.resultaattypen
            .map { ztcClientService.readResultaattype(it).omschrijving }
            .toSet()
        return ZaaktypeConfiguration().apply {
            zaaktypeUuid = newZaaktype.url.extractUuid()
            zaaktypeOmschrijving = newZaaktype.omschrijving
            creatiedatum = ZonedDateTime.now()
            groepID = previous.groepID
            defaultBehandelaarId = previous.defaultBehandelaarId
            productaanvraagtype = previous.productaanvraagtype
            isSmartDocumentsEnabled = previous.isSmartDocumentsEnabled
            einddatumGeplandWaarschuwing = previous.einddatumGeplandWaarschuwing.takeIf {
                newZaaktype.isServicenormAvailable()
            }
            uiterlijkeEinddatumAfdoeningWaarschuwing = previous.uiterlijkeEinddatumAfdoeningWaarschuwing
            nietOntvankelijkResultaattypeOmschrijving = previous.nietOntvankelijkResultaattypeOmschrijving
                ?.takeIf { it in newResultaattypeOmschrijvingen }
            setZaakbeeindigParameters(copyZaakbeeindigParameters(previous, newResultaattypeOmschrijvingen))
            zaaktypeBetrokkeneParameters = copyBetrokkeneParameters(previous, this)
            zaaktypeBrpParameters = copyBrpParameters(previous, this)
            zaaktypeEmailParameters = previous.zaaktypeEmailParameters?.let { copyEmailParameters(it, this) }
            setMailtemplateKoppelingen(copyMailtemplateKoppelingen(previous))
            setZaakAfzenders(copyZaakAfzenders(previous))
            previous.processBinding?.let { bindTo(it.processEngine, it.definitionKey) }
            previous.cmmnExtension?.let { copyCmmnExtension(it, getOrCreateCmmnExtension()) }
        }
    }

    private fun copyZaakbeeindigParameters(
        previous: ZaaktypeConfiguration,
        newResultaattypeOmschrijvingen: Set<String>
    ) = previous.getZaakbeeindigParameters()
        .filter { it.resultaattypeOmschrijving in newResultaattypeOmschrijvingen }
        .map { previousParameter ->
            ZaaktypeCompletionParameters().apply {
                zaakbeeindigReden = previousParameter.zaakbeeindigReden
                resultaattypeOmschrijving = previousParameter.resultaattypeOmschrijving
            }
        }

    private fun copyBetrokkeneParameters(previous: ZaaktypeConfiguration, next: ZaaktypeConfiguration) =
        ZaaktypeBetrokkeneParameters().apply {
            zaaktypeConfiguration = next
            isBrpKoppelenEnabled = previous.zaaktypeBetrokkeneParameters?.isBrpKoppelenEnabled
            isKvkKoppelenEnabled = previous.zaaktypeBetrokkeneParameters?.isKvkKoppelenEnabled
        }

    private fun copyBrpParameters(previous: ZaaktypeConfiguration, next: ZaaktypeConfiguration) =
        ZaaktypeBrpParameters().apply {
            zaaktypeConfiguration = next
            zoekWaarde = previous.zaaktypeBrpParameters?.zoekWaarde
            raadpleegWaarde = previous.zaaktypeBrpParameters?.raadpleegWaarde
            verwerkingregisterWaarde = previous.zaaktypeBrpParameters?.verwerkingregisterWaarde
        }

    private fun copyEmailParameters(previous: ZaaktypeEmailParameters, next: ZaaktypeConfiguration) =
        ZaaktypeEmailParameters().apply {
            zaaktypeConfiguration = next
            isEnabled = previous.isEnabled
            templateName = previous.templateName
            emailSender = previous.emailSender
            emailReply = previous.emailReply
        }

    private fun copyMailtemplateKoppelingen(previous: ZaaktypeConfiguration) =
        previous.getMailtemplateKoppelingen().map {
            ZaaktypeMailtemplateParameters().apply { mailTemplate = it.mailTemplate }
        }

    private fun copyZaakAfzenders(previous: ZaaktypeConfiguration) =
        previous.getZaakAfzenders().map {
            ZaaktypeZaakafzenderParameters().apply {
                isDefaultMail = it.isDefaultMail
                mail = it.mail
                replyTo = it.replyTo
            }
        }

    private fun copyCmmnExtension(previous: ZaaktypeCmmnExtension, next: ZaaktypeCmmnExtension) {
        next.intakeMail = previous.intakeMail
        next.afrondenMail = previous.afrondenMail
        previous.getHumanTaskParametersCollection().map {
            ZaaktypeCmmnHumantaskParameters().apply {
                doorlooptijd = it.doorlooptijd
                isActief = it.isActief
                setFormulierDefinitieID(it.getFormulierDefinitieID())
                planItemDefinitionID = it.planItemDefinitionID
                groepID = it.groepID
                setReferentieTabellen(it.getReferentieTabellen())
            }
        }.toSet().let(next::setHumanTaskParametersCollection)
        previous.getUserEventListenerParametersCollection().map {
            ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = it.planItemDefinitionID
                toelichting = it.toelichting
            }
        }.toSet().let(next::setUserEventListenerParametersCollection)
    }
}
