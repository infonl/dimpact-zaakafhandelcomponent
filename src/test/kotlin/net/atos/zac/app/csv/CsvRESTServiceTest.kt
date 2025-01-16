/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.csv

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.StreamingOutput
import net.atos.zac.app.zoeken.converter.RestZoekParametersConverter
import net.atos.zac.app.zoeken.createRESTZoekParameters
import net.atos.zac.app.zoeken.createZoekParameters
import net.atos.zac.app.zoeken.createZoekResultaatForZaakZoekObjecten
import net.atos.zac.csv.CsvService
import net.atos.zac.policy.PolicyService
import net.atos.zac.zoeken.ZoekenService

class CsvRESTServiceTest : BehaviorSpec({
    val zoekenService = mockk<ZoekenService>()
    val restZoekParametersConverter = mockk<RestZoekParametersConverter>()
    val csvService = mockk<CsvService>()
    val policyService = mockk<PolicyService>()
    val csvRESTService = CsvRESTService(
        zoekenService,
        restZoekParametersConverter,
        csvService,
        policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("The CSV REST service") {
        val restZoekParameters = createRESTZoekParameters()
        val zoekParameters = createZoekParameters()
        val zoekResultaat = createZoekResultaatForZaakZoekObjecten()
        val csvStreamingOutput = mockk<StreamingOutput>()

        every { policyService.readWerklijstRechten().zakenTakenExporteren } returns true
        every { restZoekParametersConverter.convert(restZoekParameters) } returns zoekParameters
        every { zoekenService.zoek(zoekParameters) } returns zoekResultaat
        every { csvService.exportToCsv(zoekResultaat) } returns csvStreamingOutput

        When("the download CSV function is called") {
            val response = csvRESTService.downloadCSV(restZoekParameters)

            Then("a CSV with the search results is returned") {
                response.status shouldBe 200
                response.entity shouldBe csvStreamingOutput
            }
        }
    }
})
