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
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.converter.RestZaaktypeConfigurationConverter
import nl.info.zac.app.admin.model.createRestZaaktypeConfiguration
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import java.time.LocalDate

class RestZaaktypeConverterTest : BehaviorSpec({
    val zaakafhandelParametersConverter = mockk<RestZaaktypeConfigurationConverter>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()

    val restZaaktypeConverter = RestZaaktypeConverter(
        zaakafhandelParametersConverter,
        zaaktypeConfigurationService
    )

    given("CMMN zaaktype with a versiedatum that differs from its begin geldigheid") {
        val versiedatum = LocalDate.of(2026, 2, 1)
        val beginGeldigheid = LocalDate.of(2026, 3, 10)
        val zaaktype = createZaakType(versieDatum = versiedatum, beginGeldigheid = beginGeldigheid)
        val zaaktypeUuid = zaaktype.url.extractUuid()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val restZaakafhandelParameters = createRestZaaktypeConfiguration()

        every { zaaktypeConfigurationService.findConfiguration(zaaktypeUuid) } returns zaaktypeCmmnConfiguration
        every {
            zaakafhandelParametersConverter.toRestZaaktypeConfiguration(zaaktypeCmmnConfiguration, true, zaaktype)
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
                    hasServicenorm shouldBe false
                    this.versiedatum shouldBe versiedatum
                    this.beginGeldigheid shouldBe beginGeldigheid
                    eindeGeldigheid shouldBe null
                    vertrouwelijkheidaanduiding shouldBe RestVertrouwelijkheidaanduiding.OPENBAAR
                    isNuGeldig shouldBe true
                    isOpschortingMogelijk shouldBe null
                    isVerlengingMogelijk shouldBe null
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
        val restZaakafhandelParameters = createRestZaaktypeConfiguration()

        every { zaaktypeConfigurationService.findConfiguration(zaaktypeUuid) } returns zaaktypeBpmnConfiguration
        every {
            zaakafhandelParametersConverter.toRestZaaktypeConfiguration(zaaktypeBpmnConfiguration, true, zaaktype)
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
                    hasServicenorm shouldBe false
                    versiedatum!! shouldHaveSameDayAs now
                    beginGeldigheid!! shouldHaveSameDayAs now
                    eindeGeldigheid shouldBe null
                    vertrouwelijkheidaanduiding shouldBe RestVertrouwelijkheidaanduiding.OPENBAAR
                    isNuGeldig shouldBe true
                    isOpschortingMogelijk shouldBe null
                    isVerlengingMogelijk shouldBe null
                    verlengingstermijn shouldBe null
                    zaaktypeRelaties shouldBe emptyList()
                    informatieobjecttypes shouldBe zaaktype.informatieobjecttypen.map { it.extractUuid() }
                    zaakafhandelparameters shouldBe restZaakafhandelParameters
                }
            }
        }
    }
})
