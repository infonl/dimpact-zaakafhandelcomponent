/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ProcessingException
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import java.util.UUID

class ResultaattypeOmschrijvingBackfillTest : BehaviorSpec({
    val zaaktypeConfigurationRepository = mockk<ZaaktypeConfigurationRepository>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val ztcClientService = mockk<ZtcClientService>()
    val resultaattypeOmschrijvingBackfill = ResultaattypeOmschrijvingBackfill(
        zaaktypeConfigurationRepository = zaaktypeConfigurationRepository,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        ztcClientService = ztcClientService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("resultaattype references without omschrijving, of which ZTC can read one resultaattype but not the other two") {
        val readableResultaattypeUuid = UUID.randomUUID()
        val deletedResultaattypeUuid = UUID.randomUUID()
        val unreachableResultaattypeUuid = UUID.randomUUID()
        every {
            zaaktypeConfigurationRepository.listResultaattypenWithoutOmschrijving()
        } returns listOf(readableResultaattypeUuid, deletedResultaattypeUuid, unreachableResultaattypeUuid)
        every {
            ztcClientService.readResultaattype(readableResultaattypeUuid)
        } returns createResultaatType(omschrijving = "fakeToegekend")
        every { ztcClientService.readResultaattype(deletedResultaattypeUuid) } throws NotFoundException()
        every {
            ztcClientService.readResultaattype(unreachableResultaattypeUuid)
        } throws ProcessingException("fakeConnectionRefused")
        every {
            zaaktypeConfigurationRepository.fillResultaattypeOmschrijving(readableResultaattypeUuid, "fakeToegekend")
        } returns 2
        every { zaaktypeConfigurationService.clearManagedCache() } returns "fakeCleared"
        every { zaaktypeConfigurationRepository.countResultaattypeReferencesWithoutOmschrijving() } returns 3L

        `when`("the backfill runs") {
            resultaattypeOmschrijvingBackfill.backfill()

            then(
                """the references to the readable resultaattype are filled, the others stay empty for the next start,
                    and the cached configurations are cleared"""
            ) {
                verify(exactly = 1) {
                    zaaktypeConfigurationRepository.fillResultaattypeOmschrijving(readableResultaattypeUuid, "fakeToegekend")
                    zaaktypeConfigurationService.clearManagedCache()
                }
                verify(exactly = 0) {
                    zaaktypeConfigurationRepository.fillResultaattypeOmschrijving(deletedResultaattypeUuid, any())
                    zaaktypeConfigurationRepository.fillResultaattypeOmschrijving(unreachableResultaattypeUuid, any())
                }
            }
        }
    }

    given("no resultaattype references without omschrijving, as after a previous run") {
        clearAllMocks()
        every { zaaktypeConfigurationRepository.listResultaattypenWithoutOmschrijving() } returns emptyList()

        `when`("the backfill runs") {
            resultaattypeOmschrijvingBackfill.backfill()

            then("nothing is read, filled or cleared") {
                verify(exactly = 0) {
                    ztcClientService.readResultaattype(any<UUID>())
                    zaaktypeConfigurationRepository.fillResultaattypeOmschrijving(any(), any())
                    zaaktypeConfigurationService.clearManagedCache()
                }
            }
        }
    }
})
