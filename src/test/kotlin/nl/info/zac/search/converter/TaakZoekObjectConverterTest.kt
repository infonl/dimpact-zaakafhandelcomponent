/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.task.model.TaakStatus
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.util.UUID

class TaakZoekObjectConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()

    val taakZoekObjectConverter = TaakZoekObjectConverter(
        identityService = identityService,
        flowableTaskService = flowableTaskService,
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService
    )

    mockkStatic(TaakVariabelenService::class)
    mockkStatic(TaskUtil::class)

    afterSpec {
        unmockkStatic(TaakVariabelenService::class)
        unmockkStatic(TaskUtil::class)
    }

    afterEach { checkUnnecessaryStub() }

    context("supports") {
        given("any ZoekObjectType") {
            `when`("supports is called with ZoekObjectType.TAAK") {
                then("it returns true") {
                    taakZoekObjectConverter.supports(ZoekObjectType.TAAK) shouldBe true
                }
            }

            `when`("supports is called with ZoekObjectType.ZAAK") {
                then("it returns false") {
                    taakZoekObjectConverter.supports(ZoekObjectType.ZAAK) shouldBe false
                }
            }

            `when`("supports is called with ZoekObjectType.DOCUMENT") {
                then("it returns false") {
                    taakZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) shouldBe false
                }
            }
        }
    }

    context("convert") {
        val fakeTaskId = "fakeTaskId"
        val zaakUUID = UUID.randomUUID()
        val zaaktypeUUID = UUID.randomUUID()

        given("a task with an assignee and a group") {
            val taskInfo = mockk<TaskInfo>()
            val zaak = createZaak()
            val zaakType = createZaakType(
                uri = zaak.zaaktype,
                identification = "fakeZaaktypeIdentificatie"
            )
            val candidateIdentityLink = mockk<IdentityLinkInfo>()

            every { flowableTaskService.readTask(fakeTaskId) } returns taskInfo
            every { TaakVariabelenService.readZaakUUID(taskInfo) } returns zaakUUID
            every { TaakVariabelenService.readZaakIdentificatie(taskInfo) } returns "fakeZaakIdentificatie"
            every { TaakVariabelenService.readZaaktypeUUID(taskInfo) } returns zaaktypeUUID
            every { TaakVariabelenService.readTaskData(taskInfo) } returns mapOf()
            every { TaakVariabelenService.readTaskInformation(taskInfo) } returns mapOf()
            every { TaskUtil.getTaakStatus(taskInfo) } returns TaakStatus.TOEGEKEND
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaakType

            every { taskInfo.name } returns "fakeTaskName"
            every { taskInfo.description } returns "fakeToelichting"
            every { taskInfo.createTime } returns null
            every { taskInfo.claimTime } returns null
            every { taskInfo.dueDate } returns null
            every { taskInfo.assignee } returns "fakeAssigneeId"

            every { candidateIdentityLink.type } returns IdentityLinkType.CANDIDATE
            every { candidateIdentityLink.groupId } returns "fakeGroupId"
            every { taskInfo.identityLinks } returns listOf(candidateIdentityLink)

            every { identityService.readUser("fakeAssigneeId") } returns createUser(
                id = "fakeAssigneeId",
                firstName = "Fake",
                lastName = "User",
                fullName = "Fake User"
            )
            every { identityService.readGroup("fakeGroupId") } returns createGroup(
                id = "fakeGroupId",
                name = "Fake Group"
            )

            `when`("convert is called") {
                val taakZoekObject = taakZoekObjectConverter.convert(fakeTaskId)

                then("all fields are correctly populated") {
                    taakZoekObject.getObjectId() shouldBe fakeTaskId
                    taakZoekObject.getType() shouldBe ZoekObjectType.TAAK
                    taakZoekObject.naam shouldBe "fakeTaskName"
                    taakZoekObject.toelichting shouldBe "fakeToelichting"
                    taakZoekObject.zaakUUID shouldBe zaakUUID.toString()
                    taakZoekObject.zaakIdentificatie shouldBe "fakeZaakIdentificatie"
                    taakZoekObject.zaaktypeIdentificatie shouldBe "fakeZaaktypeIdentificatie"
                    taakZoekObject.isToegekend shouldBe true
                    taakZoekObject.behandelaarGebruikersnaam shouldBe "fakeAssigneeId"
                    taakZoekObject.groepID shouldBe "fakeGroupId"
                }
            }
        }

        given("a task without an assignee or group") {
            val taskInfo = mockk<TaskInfo>()
            val zaak = createZaak()
            val zaakType = createZaakType(uri = zaak.zaaktype)

            every { flowableTaskService.readTask(fakeTaskId) } returns taskInfo
            every { TaakVariabelenService.readZaakUUID(taskInfo) } returns zaakUUID
            every { TaakVariabelenService.readZaakIdentificatie(taskInfo) } returns "fakeZaakIdentificatie"
            every { TaakVariabelenService.readZaaktypeUUID(taskInfo) } returns zaaktypeUUID
            every { TaakVariabelenService.readTaskData(taskInfo) } returns mapOf()
            every { TaakVariabelenService.readTaskInformation(taskInfo) } returns mapOf()
            every { TaskUtil.getTaakStatus(taskInfo) } returns TaakStatus.NIET_TOEGEKEND
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaakType

            every { taskInfo.name } returns "fakeTaskName"
            every { taskInfo.description } returns null
            every { taskInfo.createTime } returns null
            every { taskInfo.claimTime } returns null
            every { taskInfo.dueDate } returns null
            every { taskInfo.assignee } returns null
            every { taskInfo.identityLinks } returns emptyList()

            `when`("convert is called") {
                val taakZoekObject = taakZoekObjectConverter.convert(fakeTaskId)

                then("assignee and group fields are null and isToegekend is false") {
                    taakZoekObject.behandelaarNaam.shouldBeNull()
                    taakZoekObject.behandelaarGebruikersnaam.shouldBeNull()
                    taakZoekObject.groepID.shouldBeNull()
                    taakZoekObject.groepNaam.shouldBeNull()
                    taakZoekObject.isToegekend shouldBe false
                }
            }
        }
    }
})
