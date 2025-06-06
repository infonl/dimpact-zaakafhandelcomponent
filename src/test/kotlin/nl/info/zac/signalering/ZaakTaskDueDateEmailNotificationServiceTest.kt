/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.app.search.createZoekResultaatForZaakZoekObjecten
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringInstellingen
import org.flowable.task.api.Task
import java.net.URI
import java.util.UUID

class ZaakTaskDueDateEmailNotificationServiceTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val configuratieService = mockk<ConfiguratieService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val searchService = mockk<SearchService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val zaakTaskDueDateEmailNotificationService = ZaakTaskDueDateEmailNotificationService(
        signaleringService,
        configuratieService,
        ztcClientService,
        zaakafhandelParameterService,
        searchService,
        flowableTaskService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("An open zaak which is approaching its target date and for which a signalering was not yet sent") {
        val defaultCatalogusURI = URI("https://example.com/dummeCatalogusURI")
        val zaakTypeUUID1 = UUID.randomUUID()
        val zaakTypeUUID2 = UUID.randomUUID()
        val zaakType1 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID1"),
            omschrijving = "fakeZaakTypeOmschrijving1"
        )
        val zaakType2 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID2"),
            omschrijving = "fakeZaakTypeOmschrijving2"
        )
        val zaakTypen = listOf(zaakType1, zaakType2)
        val zaakAfhandelParameters1 = createZaakafhandelParameters(
            zaaktypeUUID = zaakTypeUUID1,
            einddatumGeplandWaarschuwing = 1
        )
        val zaakAfhandelParameters2 = createZaakafhandelParameters(
            zaaktypeUUID = zaakTypeUUID2
        )
        val assigneeName = "fakeAssignee"
        val zaakVerlopendSignaleringType = SignaleringType().apply {
            type = SignaleringType.Type.ZAAK_VERLOPEND
            subjecttype = SignaleringSubject.ZAAK
        }
        val signaleringInstellingen = createSignaleringInstellingen(
            type = zaakVerlopendSignaleringType,
            ownerType = SignaleringTarget.USER,
            ownerId = assigneeName
        )
        val zaakVerlopendSignalering = createSignalering(
            type = zaakVerlopendSignaleringType,
            zaak = createZaak()
        )
        val zoekResultaat = createZoekResultaatForZaakZoekObjecten(
            items = listOf(createZaakZoekObject(behandelaarGebruikersnaam = assigneeName))
        )

        every { configuratieService.readDefaultCatalogusURI() } returns defaultCatalogusURI
        every { ztcClientService.listZaaktypen(defaultCatalogusURI) } returns zaakTypen
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID1) } returns zaakAfhandelParameters1
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID2) } returns zaakAfhandelParameters2
        every { flowableTaskService.listOpenTasksDueNow() } returns emptyList()
        every {
            signaleringService.readInstellingenUser(SignaleringType.Type.ZAAK_VERLOPEND, assigneeName)
        } returns signaleringInstellingen
        // no signalering was sent yet
        every { signaleringService.findSignaleringVerzonden(any()) } returns null
        every { signaleringService.signaleringInstance(any<SignaleringType.Type>()) } returns zaakVerlopendSignalering
        every { signaleringService.sendSignalering(zaakVerlopendSignalering) } just runs
        every { signaleringService.createSignaleringVerzonden(zaakVerlopendSignalering) } returns mockk()
        every { flowableTaskService.listOpenTasksDueLater() } returns emptyList()
        every { searchService.zoek(any()) } returns zoekResultaat
        every { signaleringService.deleteSignaleringVerzonden(any()) } returns true

        When("the send due date email notifications method is called") {
            zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()

            Then("one zaak due date email notifications should be sent") {
                verify(exactly = 1) {
                    signaleringService.sendSignalering(zaakVerlopendSignalering)
                    signaleringService.createSignaleringVerzonden(zaakVerlopendSignalering)
                    signaleringService.deleteSignaleringVerzonden(any())
                }
            }
        }
    }
    Given("An open task which is due now and for which a signalering was not yet sent") {
        val defaultCatalogusURI = URI("https://example.com/dummeCatalogusURI")
        val zaakTypeUUID1 = UUID.randomUUID()
        val zaakTypeUUID2 = UUID.randomUUID()
        val zaakType1 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID1"),
            omschrijving = "fakeZaakTypeOmschrijving1"
        )
        val zaakType2 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID2"),
            omschrijving = "fakeZaakTypeOmschrijving2"
        )
        val zaakTypen = listOf(zaakType1, zaakType2)
        val zaakAfhandelParameters1 = createZaakafhandelParameters(zaaktypeUUID = zaakTypeUUID1)
        val zaakAfhandelParameters2 = createZaakafhandelParameters(zaaktypeUUID = zaakTypeUUID2)
        val assigneeName = "fakeAssignee"
        val openTask = mockk<Task>()
        every { openTask.assignee } returns assigneeName
        every { openTask.id } returns "fakeTaskId"
        val taakVerlopenSignaleringType = SignaleringType().apply {
            type = SignaleringType.Type.TAAK_VERLOPEN
            subjecttype = SignaleringSubject.TAAK
        }
        val signaleringInstellingen = createSignaleringInstellingen(
            type = taakVerlopenSignaleringType,
            ownerType = SignaleringTarget.USER,
            ownerId = assigneeName
        )
        val taakOpNaamVerlopenSignalering1 = createSignalering(
            type = taakVerlopenSignaleringType,
            zaak = null,
            taskInfo = openTask
        )
        every { configuratieService.readDefaultCatalogusURI() } returns defaultCatalogusURI
        every { ztcClientService.listZaaktypen(defaultCatalogusURI) } returns zaakTypen
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID1) } returns zaakAfhandelParameters1
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID2) } returns zaakAfhandelParameters2
        every { flowableTaskService.listOpenTasksDueNow() } returns listOf(openTask)
        every {
            signaleringService.readInstellingenUser(any<SignaleringType.Type>(), assigneeName)
        } returns signaleringInstellingen
        // signalering was not yet sent
        every { signaleringService.findSignaleringVerzonden(any()) } returns null
        every { signaleringService.signaleringInstance(any<SignaleringType.Type>()) } returns taakOpNaamVerlopenSignalering1
        every { signaleringService.sendSignalering(taakOpNaamVerlopenSignalering1) } just runs
        every { signaleringService.createSignaleringVerzonden(taakOpNaamVerlopenSignalering1) } returns mockk()
        every { flowableTaskService.listOpenTasksDueLater() } returns emptyList()

        When("the send due date email notifications method is called") {
            zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()

            Then("one task due date email notifications should be sent") {
                verify(exactly = 1) {
                    signaleringService.sendSignalering(taakOpNaamVerlopenSignalering1)
                    signaleringService.createSignaleringVerzonden(taakOpNaamVerlopenSignalering1)
                }
            }
        }
    }
})
