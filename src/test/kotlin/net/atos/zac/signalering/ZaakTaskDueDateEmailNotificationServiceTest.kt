package net.atos.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.createZaakafhandelParameters
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

    Given("One open task which is due now") {
        val defaultCatalogusURI = URI("http://example.com/dummeCatalogusURI")
        val zaakTypeUUID1 = UUID.randomUUID()
        val zaakTypeUUID2 = UUID.randomUUID()
        val zaakType1 = createZaakType(
            uri = URI("http://example.com/zaaktypes/$zaakTypeUUID1"),
            omschrijving = "dummyZaakTypeOmschrijving1"
        )
        val zaakType2 = createZaakType(
            uri = URI("http://example.com/zaaktypes/$zaakTypeUUID2"),
            omschrijving = "dummyZaakTypeOmschrijving2"
        )
        val zaakTypen = listOf(
            zaakType1,
            zaakType2
        )
        val zaakAfhandelParameters1 = createZaakafhandelParameters(
            zaaktypeUUID = zaakTypeUUID1
        )
        val zaakAfhandelParameters2 = createZaakafhandelParameters(
            zaaktypeUUID = zaakTypeUUID2
        )
        val assignee1 = "dummyAssignee1"
        val openTask = mockk<Task>()
        every { openTask.assignee } returns assignee1
        every { openTask.id } returns "dummyTaskId"
        val taakVerlopenSignaleringType = SignaleringType().apply {
            type = SignaleringType.Type.TAAK_VERLOPEN
            subjecttype = SignaleringSubject.TAAK
        }
        val signaleringInstellingen1 = createSignaleringInstellingen(
            type = taakVerlopenSignaleringType,
            ownerType = SignaleringTarget.USER,
            ownerId = assignee1
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
            signaleringService.readInstellingenUser(any<SignaleringType.Type>(), assignee1)
        } returns signaleringInstellingen1
        every { signaleringService.findSignaleringVerzonden(any()) } returns Optional.empty()
        every { signaleringService.signaleringInstance(any<SignaleringType.Type>()) } returns taakOpNaamVerlopenSignalering1
        every { signaleringService.sendSignalering(taakOpNaamVerlopenSignalering1) } just runs
        every { signaleringService.createSignaleringVerzonden(taakOpNaamVerlopenSignalering1) } returns mockk()
        every { flowableTaskService.listOpenTasksDueLater() } returns emptyList()

        When("the send due date email notifications method is called") {
            zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()

            Then("one task due date email notifications is sent") {
                verify(exactly = 1) {
                    signaleringService.sendSignalering(taakOpNaamVerlopenSignalering1)
                    signaleringService.createSignaleringVerzonden(taakOpNaamVerlopenSignalering1)
                }
            }
        }
    }
})
