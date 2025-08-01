/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTHumanTaskParametersConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter
import net.atos.zac.app.admin.model.RESTCaseDefinition
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.app.admin.createRestZaakAfhandelParameters
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.smartdocuments.SmartDocumentsService
import java.time.LocalDate

class RestZaakafhandelParametersConverterTest : BehaviorSpec({
    val caseDefinitionConverter = mockk<RESTCaseDefinitionConverter>()
    val zaakbeeindigParameterConverter = mockk<RESTZaakbeeindigParameterConverter>()
    val restHumanTaskParametersConverter = mockk<RESTHumanTaskParametersConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val smartDocumentsService = mockk<SmartDocumentsService>()

    val restZaakafhandelParametersConverter = RestZaakafhandelParametersConverter(
        caseDefinitionConverter,
        zaakbeeindigParameterConverter,
        restHumanTaskParametersConverter,
        ztcClientService,
        zaakafhandelParameterService,
        smartDocumentsService
    )

    Given("ZaakafhandelParameters with minimal content") {
        val zaakafhandelParameters = createZaakafhandelParameters()
        val zaakType = createZaakType().apply {
            beginGeldigheid = LocalDate.now().minusDays(1)
        }
        every { ztcClientService.readZaaktype(zaakafhandelParameters.zaakTypeUUID) } returns zaakType
        every { smartDocumentsService.isEnabled() } returns true
        every {
            caseDefinitionConverter.convertToRESTCaseDefinition(
                zaakafhandelParameters.caseDefinitionID,
                true
            )
        } returns null

        When("converted to REST representation") {
            val restZaakafhandelParameters = restZaakafhandelParametersConverter.toRestZaakafhandelParameters(
                zaakafhandelParameters,
                true
            )

            Then("the created object is correct") {
                with(restZaakafhandelParameters) {
                    id shouldBe zaakafhandelParameters.id
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
                    zaakNietOntvankelijkResultaattype shouldBe null
                    // default value should be set
                    intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    // default value should be set
                    afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    productaanvraagtype shouldBe null
                    domein shouldBe "fakeDomein"
                    valide shouldBe false
                    humanTaskParameters shouldBe emptyList()
                    userEventListenerParameters shouldBe emptyList()
                    mailtemplateKoppelingen shouldBe emptyList()
                    zaakbeeindigParameters shouldBe emptyList()
                    zaakAfzenders shouldBe emptyList()
                    smartDocuments shouldBe RestSmartDocuments(
                        enabledGlobally = true,
                        enabledForZaaktype = false
                    )
                }
            }
        }
    }

    Given("RestZaakafhandelParameters with minimal content") {
        val restResultType = createResultaatType().toRestResultaatType()
        val restZaakafhandelParameters = createRestZaakAfhandelParameters().apply {
            caseDefinition = RESTCaseDefinition()
            zaakNietOntvankelijkResultaattype = restResultType
        }
        val zaakafhandelParameters = createZaakafhandelParameters()
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(restZaakafhandelParameters.zaaktype.uuid)
        } returns zaakafhandelParameters
        every { restHumanTaskParametersConverter.convertRESTHumanTaskParameters(any()) } returns emptyList()

        When("converted to DB model representation") {
            val zaakafhandelParameters = restZaakafhandelParametersConverter.toZaakafhandelParameters(
                restZaakafhandelParameters
            )

            Then("the created object is correct") {
                with(zaakafhandelParameters) {
                    id shouldBe restZaakafhandelParameters.id
                    zaakTypeUUID shouldBe restZaakafhandelParameters.zaaktype.uuid
                    zaaktypeOmschrijving shouldBe "fakeOmschrijving"
                    caseDefinitionID shouldBe null
                    groepID shouldBe null
                    gebruikersnaamMedewerker shouldBe null
                    einddatumGeplandWaarschuwing shouldBe null
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe null
                    nietOntvankelijkResultaattype shouldBe restResultType.id
                    creatiedatum shouldNotBe null
                    intakeMail shouldBe null
                    afrondenMail shouldBe null
                    productaanvraagtype shouldBe null
                    domein shouldBe "fakeDomein"
                    isSmartDocumentsIngeschakeld shouldBe false
                }
            }
        }
    }
})
