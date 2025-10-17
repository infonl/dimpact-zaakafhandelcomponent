/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.csv

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.StreamingOutput
import net.atos.zac.csv.CsvService
import nl.info.zac.app.search.converter.RestZoekParametersConverter
import nl.info.zac.app.search.createRESTZoekParameters
import nl.info.zac.app.search.createZoekParameters
import nl.info.zac.app.search.createZoekResultaatForZaakZoekObjecten
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.SearchService

class CsvRestServiceTest : BehaviorSpec({
    val searchService = mockk<SearchService>()
    val restZoekParametersConverter = mockk<RestZoekParametersConverter>()
    val csvService = mockk<CsvService>()
    val policyService = mockk<PolicyService>()
    val csvRESTService = CsvRestService(
        searchService,
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
        every { searchService.zoek(zoekParameters) } returns zoekResultaat
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
