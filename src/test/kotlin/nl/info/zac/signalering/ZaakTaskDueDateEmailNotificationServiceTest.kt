/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import nl.info.zac.app.search.model.createZoekResultaatForZaakZoekObjecten
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringInstellingen
import org.flowable.task.api.Task
import java.net.URI
import java.util.UUID

class ZaakTaskDueDateEmailNotificationServiceTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val configurationService = mockk<ConfigurationService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val searchService = mockk<SearchService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val zaakTaskDueDateEmailNotificationService = ZaakTaskDueDateEmailNotificationService(
        signaleringService,
        configurationService,
        ztcClientService,
        zaaktypeConfigurationService,
        searchService,
        flowableTaskService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        given(
            "An open zaak of a $configurationType zaaktype which is approaching its target date and for which " +
                "a signalering was not yet sent"
        ) {
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
            val zaaktypeConfiguration1 = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                zaaktypeUuid = zaakTypeUUID1
                einddatumGeplandWaarschuwing = 1
            }
            val zaaktypeConfiguration2 = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                zaaktypeUuid = zaakTypeUUID2
            }
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

            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogusURI
            every { ztcClientService.listZaaktypen(defaultCatalogusURI) } returns zaakTypen
            every {
                zaaktypeConfigurationService.findConfiguration(zaakTypeUUID1)
            } returns zaaktypeConfiguration1
            every {
                zaaktypeConfigurationService.findConfiguration(zaakTypeUUID2)
            } returns zaaktypeConfiguration2
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
            var isSystemUserDuringCronWork: Boolean? = null
            every { searchService.search(any()) } answers {
                isSystemUserDuringCronWork = LoggedInUserProvider.systemUser.get()
                zoekResultaat
            }
            every { signaleringService.deleteSignaleringVerzonden(any()) } returns true

            `when`("the send due date email notifications method is called") {
                zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()
                val wasSystemUserAfterCronWork = LoggedInUserProvider.systemUser.get()

                then("one zaak due date email notifications should be sent") {
                    verify(exactly = 1) {
                        signaleringService.sendSignalering(zaakVerlopendSignalering)
                        signaleringService.createSignaleringVerzonden(zaakVerlopendSignalering)
                        signaleringService.deleteSignaleringVerzonden(any())
                    }
                }

                and("the cron work runs as the system user, and no longer does once it has finished") {
                    isSystemUserDuringCronWork shouldBe true
                    wasSystemUserAfterCronWork shouldBe false
                }
            }
        }
    }

    given("A zaaktype without a zaaktype configuration and no open tasks") {
        val defaultCatalogusURI = URI("https://example.com/fakeCatalogusURI")
        val zaakTypeUUID = UUID.randomUUID()
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaakTypeUUID"))
        every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogusURI
        every { ztcClientService.listZaaktypen(defaultCatalogusURI) } returns listOf(zaakType)
        every { zaaktypeConfigurationService.findConfiguration(zaakTypeUUID) } returns null
        every { flowableTaskService.listOpenTasksDueNow() } returns emptyList()
        every { flowableTaskService.listOpenTasksDueLater() } returns emptyList()

        `when`("the send due date email notifications method is called") {
            zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()

            then("no zaken are searched for and no signalering is sent") {
                verify(exactly = 0) {
                    searchService.search(any())
                    signaleringService.sendSignalering(any())
                }
            }
        }
    }

    given("An open task which is due now and for which a signalering was not yet sent") {
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
        val zaaktypeCmmnConfiguration1 = createZaaktypeCmmnConfiguration(zaaktypeUUID = zaakTypeUUID1)
        val zaaktypeCmmnConfiguration2 = createZaaktypeCmmnConfiguration(zaaktypeUUID = zaakTypeUUID2)
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
        every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogusURI
        every { ztcClientService.listZaaktypen(defaultCatalogusURI) } returns zaakTypen
        every {
            zaaktypeConfigurationService.findConfiguration(zaakTypeUUID1)
        } returns zaaktypeCmmnConfiguration1
        every {
            zaaktypeConfigurationService.findConfiguration(zaakTypeUUID2)
        } returns zaaktypeCmmnConfiguration2
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

        `when`("the send due date email notifications method is called") {
            zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()

            then("one task due date email notifications should be sent") {
                verify(exactly = 1) {
                    signaleringService.sendSignalering(taakOpNaamVerlopenSignalering1)
                    signaleringService.createSignaleringVerzonden(taakOpNaamVerlopenSignalering1)
                }
            }
        }
    }
})
