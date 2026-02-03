/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTHumanTaskParametersConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter
import net.atos.zac.app.admin.model.RESTCaseDefinition
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.createRestZaakafhandelParameters
import nl.info.zac.app.admin.createRestZaakbeeindigParameter
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.admin.model.RestZaakAfzender
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.smartdocuments.SmartDocumentsService
import java.time.LocalDate

class RestZaakafhandelParametersConverterTest : BehaviorSpec({
    val caseDefinitionConverter = mockk<RESTCaseDefinitionConverter>()
    val zaakbeeindigParameterConverter = mockk<RESTZaakbeeindigParameterConverter>()
    val restHumanTaskParametersConverter = mockk<RESTHumanTaskParametersConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val smartDocumentsService = mockk<SmartDocumentsService>()

    val restZaakafhandelParametersConverter = RestZaakafhandelParametersConverter(
        caseDefinitionConverter,
        zaakbeeindigParameterConverter,
        restHumanTaskParametersConverter,
        ztcClientService,
        zaaktypeCmmnConfigurationService,
        smartDocumentsService
    )

    Given("ZaakafhandelParameters CMMN with minimal content") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val zaakType = createZaakType().apply {
            beginGeldigheid = LocalDate.now().minusDays(1)
        }
        val resultaatType = createResultaatType()
        val restResultType = resultaatType.toRestResultaatType()
        val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)

        every { ztcClientService.readZaaktype(zaaktypeCmmnConfiguration.zaaktypeUuid) } returns zaakType
        every {
            ztcClientService.readResultaattype(zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype!!)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns listOf(restZaakbeeindigParameter)
        every { smartDocumentsService.isEnabled() } returns true
        every {
            caseDefinitionConverter.convertToRESTCaseDefinition(
                zaaktypeCmmnConfiguration.caseDefinitionID,
                true
            )
        } returns null

        When("converted to REST representation") {
            val restZaakafhandelParameters = restZaakafhandelParametersConverter.toRestZaaktypeCmmnConfiguration(
                zaaktypeCmmnConfiguration,
                true
            )

            Then("the created object is correct") {
                with(restZaakafhandelParameters) {
                    id shouldBe zaaktypeCmmnConfiguration.id
                    with(zaaktype) {
                        uuid shouldBe zaakType.url.extractUuid()
                        identificatie shouldBe zaakType.identificatie
                        doel shouldBe zaakType.doel
                        omschrijving shouldBe zaakType.omschrijving
                        servicenorm shouldBe false
                        versiedatum shouldBe zaakType.versiedatum
                        beginGeldigheid shouldBe zaakType.beginGeldigheid
                        eindeGeldigheid shouldBe zaakType.eindeGeldigheid
                        vertrouwelijkheidaanduiding shouldBe zaakType.vertrouwelijkheidaanduiding
                        nuGeldig shouldBe true
                    }
                    caseDefinition shouldBe null
                    defaultBehandelaarId shouldBe null
                    defaultGroepId shouldBe null
                    einddatumGeplandWaarschuwing shouldBe null
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe null
                    creatiedatum shouldNotBe null
                    zaakNietOntvankelijkResultaattype shouldBe restResultType
                    // default value should be set
                    intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    // default value should be set
                    afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    productaanvraagtype shouldBe null
                    domein shouldBe "fakeDomein"
                    valide shouldBe false
                    humanTaskParameters shouldBe emptyList()
                    userEventListenerParameters shouldBe emptyList()
                    mailtemplateKoppelingen shouldHaveSize 1
                    zaakbeeindigParameters shouldBe listOf(restZaakbeeindigParameter)
                    zaakAfzenders shouldBe listOf(
                        RestZaakAfzender(id = null, mail = "mail@example.com", replyTo = "replyTo@example.com"),
                        RestZaakAfzender(mail = "GEMEENTE", speciaal = true),
                        RestZaakAfzender(mail = "MEDEWERKER", speciaal = true)
                    )
                    smartDocuments shouldBe RestSmartDocuments(
                        enabledGlobally = true,
                        enabledForZaaktype = false
                    )
                }
            }
        }
    }

    Given("RestZaakafhandelParameters CMMN with minimal content") {
        val restResultType = createResultaatType().toRestResultaatType()
        val restZaakafhandelParameters = createRestZaakafhandelParameters().apply {
            caseDefinition = RESTCaseDefinition()
            zaakNietOntvankelijkResultaattype = restResultType
        }
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        every {
            zaaktypeCmmnConfigurationService.fetchZaaktypeCmmnConfiguration(restZaakafhandelParameters.zaaktype.uuid)
        } returns zaaktypeCmmnConfiguration
        every { restHumanTaskParametersConverter.convertRESTHumanTaskParameters(any()) } returns emptyList()

        When("converted to DB model representation") {
            val zaaktypeCmmnConfiguration = restZaakafhandelParametersConverter.toZaaktypeCmmnConfiguration(
                restZaakafhandelParameters
            )

            Then("the created object is correct") {
                with(zaaktypeCmmnConfiguration) {
                    id shouldBe restZaakafhandelParameters.id
                    zaaktypeUuid shouldBe restZaakafhandelParameters.zaaktype.uuid
                    zaaktypeOmschrijving shouldBe "fakeOmschrijving"
                    caseDefinitionID shouldBe null
                    groepID shouldBe "fakeGroupId"
                    defaultBehandelaarId shouldBe null
                    einddatumGeplandWaarschuwing shouldBe null
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe null
                    nietOntvankelijkResultaattype shouldBe restResultType.id
                    creatiedatum shouldNotBe null
                    intakeMail shouldBe null
                    afrondenMail shouldBe null
                    productaanvraagtype shouldBe null
                    domein shouldBe "fakeDomein"
                    smartDocumentsIngeschakeld shouldBe false
                }
            }
        }
    }

    Given("ZaakafhandelParameters BPMN with minimal content") {
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        val zaakType = createZaakType().apply {
            beginGeldigheid = LocalDate.now().minusDays(1)
        }
        val resultaatType = createResultaatType()
        val restResultType = resultaatType.toRestResultaatType()
        val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)

        every { ztcClientService.readZaaktype(zaaktypeBpmnConfiguration.zaaktypeUuid) } returns zaakType
        every {
            ztcClientService.readResultaattype(zaaktypeBpmnConfiguration.nietOntvankelijkResultaattype!!)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeBpmnConfiguration.getZaakbeeindigParameters())
        } returns listOf(restZaakbeeindigParameter)
        every { smartDocumentsService.isEnabled() } returns true

        When("converted to REST representation") {
            val restZaakafhandelParameters = restZaakafhandelParametersConverter.toRestZaaktypeBpmnConfiguration(
                zaaktypeBpmnConfiguration
            )

            Then("the created object is correct") {
                with(restZaakafhandelParameters) {
                    id shouldBe zaaktypeBpmnConfiguration.id
                    with(zaaktype) {
                        uuid shouldBe zaakType.url.extractUuid()
                        identificatie shouldBe zaakType.identificatie
                        doel shouldBe zaakType.doel
                        omschrijving shouldBe zaakType.omschrijving
                        servicenorm shouldBe false
                        versiedatum shouldBe zaakType.versiedatum
                        beginGeldigheid shouldBe zaakType.beginGeldigheid
                        eindeGeldigheid shouldBe zaakType.eindeGeldigheid
                        vertrouwelijkheidaanduiding shouldBe zaakType.vertrouwelijkheidaanduiding
                        nuGeldig shouldBe true
                    }
                    defaultGroepId shouldBe null
                    creatiedatum shouldNotBe null
                    zaakNietOntvankelijkResultaattype shouldBe restResultType
                    productaanvraagtype shouldBe null
                    domein shouldBe "fakeDomein"
                    zaakbeeindigParameters shouldBe listOf(restZaakbeeindigParameter)
                    smartDocuments shouldBe RestSmartDocuments(
                        enabledGlobally = true,
                        enabledForZaaktype = true
                    )
                }
            }
        }
    }
})
