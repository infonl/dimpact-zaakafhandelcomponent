/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.createZaakafhandelParameters
import net.atos.zac.app.zoeken.createZaakZoekObject
import net.atos.zac.app.zoeken.createZoekResultaatForZaakZoekObjecten
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.createSignalering
import net.atos.zac.signalering.model.createSignaleringInstellingen
import net.atos.zac.zoeken.ZoekenService
import org.flowable.task.api.Task
import java.net.URI
import java.util.Optional
import java.util.UUID

class ZaakTaskDueDateEmailNotificationServiceTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val configuratieService = mockk<ConfiguratieService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zoekenService = mockk<ZoekenService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val zaakTaskDueDateEmailNotificationService = ZaakTaskDueDateEmailNotificationService(
        signaleringService,
        configuratieService,
        ztcClientService,
        zaakafhandelParameterService,
        zoekenService,
        flowableTaskService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("An open zaak which is approaching it's target date") {
        val defaultCatalogusURI = URI("https://example.com/dummeCatalogusURI")
        val zaakTypeUUID1 = UUID.randomUUID()
        val zaakTypeUUID2 = UUID.randomUUID()
        val zaakType1 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID1"),
            omschrijving = "dummyZaakTypeOmschrijving1"
        )
        val zaakType2 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID2"),
            omschrijving = "dummyZaakTypeOmschrijving2"
        )
        val zaakTypen = listOf(zaakType1, zaakType2)
        val zaakAfhandelParameters1 = createZaakafhandelParameters(
            zaaktypeUUID = zaakTypeUUID1,
            einddatumGeplandWaarschuwing = 1
        )
        val zaakAfhandelParameters2 = createZaakafhandelParameters(
            zaaktypeUUID = zaakTypeUUID2
        )
        val assigneeName = "dummyAssignee"
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
        every { signaleringService.findSignaleringVerzonden(any()) } returns Optional.empty()
        every { signaleringService.signaleringInstance(any<SignaleringType.Type>()) } returns zaakVerlopendSignalering
        every { signaleringService.sendSignalering(zaakVerlopendSignalering) } just runs
        every { signaleringService.createSignaleringVerzonden(zaakVerlopendSignalering) } returns mockk()
        every { flowableTaskService.listOpenTasksDueLater() } returns emptyList()
        every { zoekenService.zoek(any()) } returns zoekResultaat
        every { signaleringService.deleteSignaleringVerzonden(any()) } just runs

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
    Given("An open task which is due now") {
        val defaultCatalogusURI = URI("https://example.com/dummeCatalogusURI")
        val zaakTypeUUID1 = UUID.randomUUID()
        val zaakTypeUUID2 = UUID.randomUUID()
        val zaakType1 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID1"),
            omschrijving = "dummyZaakTypeOmschrijving1"
        )
        val zaakType2 = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID2"),
            omschrijving = "dummyZaakTypeOmschrijving2"
        )
        val zaakTypen = listOf(zaakType1, zaakType2)
        val zaakAfhandelParameters1 = createZaakafhandelParameters(zaaktypeUUID = zaakTypeUUID1)
        val zaakAfhandelParameters2 = createZaakafhandelParameters(zaaktypeUUID = zaakTypeUUID2)
        val assigneeName = "dummyAssignee"
        val openTask = mockk<Task>()
        every { openTask.assignee } returns assigneeName
        every { openTask.id } returns "dummyTaskId"
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
        every { signaleringService.findSignaleringVerzonden(any()) } returns Optional.empty()
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
