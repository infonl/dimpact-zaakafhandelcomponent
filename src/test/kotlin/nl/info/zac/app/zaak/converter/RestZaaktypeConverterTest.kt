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
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.app.admin.model.createRestZaakafhandelParameters
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

    given("CMMN zaaktype") {
        val zaaktype = createZaakType()
        val zaaktypeUuid = zaaktype.url.extractUuid()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val now = LocalDate.now()
        val restZaakafhandelParameters = createRestZaakafhandelParameters()

        every { zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUuid) } returns null
        every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid) } returns zaaktypeCmmnConfiguration
        every {
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(zaaktypeCmmnConfiguration, true)
        } returns restZaakafhandelParameters

        `when`("converted to REST") {
            val restZaaktype = restZaaktypeConverter.convert(zaaktype)

            then("the created object is correct") {
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

    given("BPMN zaaktype") {
        val zaaktype = createZaakType()
        val zaaktypeUuid = zaaktype.url.extractUuid()
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        val now = LocalDate.now()
        val restZaakafhandelParameters = createRestZaakafhandelParameters()

        every { zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUuid) } returns zaaktypeBpmnConfiguration
        every {
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(zaaktypeBpmnConfiguration)
        } returns restZaakafhandelParameters

        `when`("converted to REST") {
            val restZaaktype = restZaaktypeConverter.convert(zaaktype)

            then("the created object is correct") {
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
