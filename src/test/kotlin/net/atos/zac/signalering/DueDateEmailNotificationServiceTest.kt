package net.atos.zac.signalering

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.createSignalering
import net.atos.zac.signalering.model.createSignaleringInstellingen
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters
import net.atos.zac.zoeken.ZoekenService
import org.flowable.task.api.Task
import java.net.URI
import java.util.Optional
import java.util.UUID

@Ignored("This test is not yet implemented")
class DueDateEmailNotificationServiceTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val configuratieService = mockk<ConfiguratieService>()
    val ztcClientService = mockk<ZTCClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zoekenService = mockk<ZoekenService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val dueDateEmailNotificationService = DueDateEmailNotificationService(
        signaleringService,
        configuratieService,
        ztcClientService,
        zaakafhandelParameterService,
        zoekenService,
        flowableTaskService
    )

    Given("Two zaaktypen") {
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
            zaakTypeUUID = zaakTypeUUID1
        )
        val zaakAfhandelParameters2 = createZaakafhandelParameters(
            zaakTypeUUID = zaakTypeUUID2
        )
        val openTask = mockk<Task>()
        val assignee1 = "dummyAssignee1"
        val signaleringInstellingen1 = createSignaleringInstellingen(
            medewerker = assignee1,
            type = SignaleringType().apply {
                type = SignaleringType.Type.TAAK_VERLOPEN
                subjecttype = SignaleringSubject.TAAK
            }
        )
        val signalering1 = createSignalering()
        every { configuratieService.readDefaultCatalogusURI() } returns defaultCatalogusURI
        every { ztcClientService.listZaaktypen(defaultCatalogusURI) } returns zaakTypen
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID1) } returns zaakAfhandelParameters1
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID2) } returns zaakAfhandelParameters2
        every { flowableTaskService.listOpenTasksDueNow() } returns listOf(openTask)
        every { openTask.assignee } returns assignee1
        every { openTask.id } returns "dummyTaskId"
        every {
            signaleringService.readInstellingenUser(SignaleringType.Type.TAAK_VERLOPEN, assignee1)
        } returns signaleringInstellingen1
        every { signaleringService.findSignaleringVerzonden(any()) } returns Optional.empty()
        every { signaleringService.signaleringInstance(SignaleringType.Type.TAAK_VERLOPEN) } returns signalering1

        When("the send due date email notifications method is called") {
            dueDateEmailNotificationService.sendDueDateEmailNotifications()

            Then("due date email notifications are sent") {
                // Test code
            }
        }
    }
})
