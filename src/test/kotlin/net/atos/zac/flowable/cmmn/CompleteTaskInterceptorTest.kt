/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import nl.info.zac.search.IndexingService
import org.flowable.cmmn.engine.CmmnEngineConfiguration
import org.flowable.task.service.impl.persistence.entity.TaskEntity

class CompleteTaskInterceptorTest : BehaviorSpec({
    val flowableHelper = mockk<FlowableHelper>()
    val indexingService = mockk<IndexingService>()
    val completeTaskInterceptor = CompleteTaskInterceptor(mockk<CmmnEngineConfiguration>())

    mockkObject(FlowableHelper)

    afterSpec {
        unmockkAll()
    }

    given("A CMMN taak that is completed without an authenticated user") {
        val taskEntity = mockk<TaskEntity> {
            every { id } returns "fakeTaskId"
        }
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.indexeerService } returns indexingService
        every { indexingService.removeTaak("fakeTaskId") } just runs

        `when`("the completion of the taak is handled") {
            completeTaskInterceptor.handleCompleteTask(taskEntity)

            then(
                "the taak is removed from the search index, so that a completed taak does not appear in " +
                    "werklijsten and zoekresultaten"
            ) {
                verify(exactly = 1) {
                    indexingService.removeTaak("fakeTaskId")
                }
            }
        }
    }
})
