/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldHaveSameDayAs
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.app.admin.createRestZaakafhandelParameters
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import java.time.LocalDate

class RestZaaktypeConverterTest : BehaviorSpec({
    val zaakafhandelParametersConverter = mockk<RestZaakafhandelParametersConverter>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()

    val restZaaktypeConverter = RestZaaktypeConverter(
        zaakafhandelParametersConverter,
        zaaktypeCmmnConfigurationService,
        zaaktypeBpmnConfigurationBeheerService
    )

    Given("CMMN zaaktype") {
        val zaaktype = createZaakType()
        val zaaktypeUuid = zaaktype.url.extractUuid()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val now = LocalDate.now()
        val restZaakafhandelParameters = createRestZaakafhandelParameters()

        every { zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUuid) } returns null
        every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid) } returns zaaktypeCmmnConfiguration
        every {
            zaakafhandelParametersConverter.toRestZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration, true)
        } returns restZaakafhandelParameters

        When("converted to REST") {
            val restZaaktype = restZaaktypeConverter.convert(zaaktype)

            Then("the created object is correct") {
                with(restZaaktype) {
                    uuid shouldBe zaaktypeUuid
                    identificatie shouldBe "fakeIdentificatie"
                    doel shouldBe "fakeDoel"
                    omschrijving shouldBe "fakeZaakTypeOmschrijving"
                    referentieproces shouldBe null
                    servicenorm shouldBe false
                    versiedatum!! shouldHaveSameDayAs now
                    beginGeldigheid!! shouldHaveSameDayAs now
                    eindeGeldigheid shouldBe null
                    vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR
                    nuGeldig shouldBe true
                    opschortingMogelijk shouldBe null
                    verlengingMogelijk shouldBe null
                    verlengingstermijn shouldBe null
                    zaaktypeRelaties shouldBe emptyList()
                    informatieobjecttypes shouldBe zaaktype.informatieobjecttypen.map { it.extractUuid() }
                    zaakafhandelparameters shouldBe restZaakafhandelParameters
                }
            }
        }
    }

    Given("BPMN zaaktype") {
        val zaaktype = createZaakType()
        val zaaktypeUuid = zaaktype.url.extractUuid()
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        val now = LocalDate.now()
        val restZaakafhandelParameters = createRestZaakafhandelParameters()

        every { zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUuid) } returns zaaktypeBpmnConfiguration
        every {
            zaakafhandelParametersConverter.toRestZaaktypeBpmnConfiguration(zaaktypeBpmnConfiguration)
        } returns restZaakafhandelParameters

        When("converted to REST") {
            val restZaaktype = restZaaktypeConverter.convert(zaaktype)

            Then("the created object is correct") {
                with(restZaaktype) {
                    uuid shouldBe zaaktypeUuid
                    identificatie shouldBe "fakeIdentificatie"
                    doel shouldBe "fakeDoel"
                    omschrijving shouldBe "fakeZaakTypeOmschrijving"
                    referentieproces shouldBe null
                    servicenorm shouldBe false
                    versiedatum!! shouldHaveSameDayAs now
                    beginGeldigheid!! shouldHaveSameDayAs now
                    eindeGeldigheid shouldBe null
                    vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR
                    nuGeldig shouldBe true
                    opschortingMogelijk shouldBe null
                    verlengingMogelijk shouldBe null
                    verlengingstermijn shouldBe null
                    zaaktypeRelaties shouldBe emptyList()
                    informatieobjecttypes shouldBe zaaktype.informatieobjecttypen.map { it.extractUuid() }
                    zaakafhandelparameters shouldBe restZaakafhandelParameters
                }
            }
        }
    }
})
