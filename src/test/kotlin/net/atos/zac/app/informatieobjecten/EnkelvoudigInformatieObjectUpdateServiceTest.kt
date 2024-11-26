/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.flowable.createTestTask
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createTaakRechten
import java.util.UUID

class EnkelvoudigInformatieObjectUpdateServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val zgwApiService = mockk<ZGWApiService>()

    val enkelvoudigInformatieObjectUpdateService = EnkelvoudigInformatieObjectUpdateService(
        drcClientService,
        enkelvoudigInformatieObjectLockService,
        flowableTaskService,
        loggedInUserInstance,
        policyService,
        taakVariabelenService,
        zgwApiService
    )

    Given("Zaak, lock request and a task") {
        val zaak = Zaak()
        val enkelvoudigInformatieObjectCreateLockRequest = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieObject = createZaakInformatieobject()
        val taskId = "1234"
        val task = createTestTask()

        every {
            zgwApiService.createZaakInformatieobjectForZaak(
                zaak,
                enkelvoudigInformatieObjectCreateLockRequest,
                enkelvoudigInformatieObjectCreateLockRequest.titel,
                enkelvoudigInformatieObjectCreateLockRequest.beschrijving,
                ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
            )
        } returns zaakInformatieObject
        every { flowableTaskService.findOpenTask(taskId) } returns task
        every { policyService.readTaakRechten(task) } returns createTaakRechten()
        every { taakVariabelenService.setTaakdocumenten(task, any<List<UUID>>()) } just runs

        When("creating information object for a task is called") {
            val zaakInfo = enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                zaak,
                enkelvoudigInformatieObjectCreateLockRequest,
                taskId
            )

            Then("correct zaak info object is returned") {
                zaakInfo shouldBe zaakInformatieObject
            }

            And("task document is set") {
                verify(exactly = 1) {
                    taakVariabelenService.setTaakdocumenten(task, any<List<UUID>>())
                }
            }
        }
    }
})
