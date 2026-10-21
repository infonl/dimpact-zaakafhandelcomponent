/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.flowable.cmmn.api.CmmnRepositoryService
import org.flowable.cmmn.api.repository.CmmnDeployment
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery

class CmmnDeployerTest : BehaviorSpec({
    val modelFileName = "Generiek_zaakafhandelmodel.cmmn.xml"
    val caseKey = "generiek-zaakafhandelmodel"
    val caseName = "Generiek zaakafhandelmodel"
    val modelBytes = CmmnDeployerTest::class.java.getResource("/cmmn/$modelFileName")?.readBytes()
        ?: error("CMMN model $modelFileName not found")

    fun mockDeploymentQuery(cmmnRepositoryService: CmmnRepositoryService, cmmnDeployment: CmmnDeployment?) {
        val cmmnDeploymentQuery = mockk<CmmnDeploymentQuery>()
        every { cmmnRepositoryService.createDeploymentQuery() } returns cmmnDeploymentQuery
        every { cmmnDeploymentQuery.deploymentKey(caseKey) } returns cmmnDeploymentQuery
        every { cmmnDeploymentQuery.latest() } returns cmmnDeploymentQuery
        every { cmmnDeploymentQuery.singleResult() } returns cmmnDeployment
    }

    fun mockDeploymentBuilder(cmmnRepositoryService: CmmnRepositoryService) =
        mockk<CmmnDeploymentBuilder>().also {
            every { cmmnRepositoryService.createDeployment() } returns it
            every { it.key(caseKey) } returns it
            every { it.name(caseName) } returns it
            every { it.addBytes(modelFileName, modelBytes) } returns it
            every { it.deploy() } returns mockk()
        }

    afterEach {
        checkUnnecessaryStub()
    }

    context("Deploying the CMMN models on startup") {
        given("a CMMN model that has not been deployed yet") {
            val cmmnRepositoryService = mockk<CmmnRepositoryService>()
            mockDeploymentQuery(cmmnRepositoryService, null)
            val cmmnDeploymentBuilder = mockDeploymentBuilder(cmmnRepositoryService)
            val cmmnDeployer = CmmnDeployer(cmmnRepositoryService)

            `when`("the application starts") {
                cmmnDeployer.onStartup(Any())

                then("the model is deployed with the key and name of its case") {
                    verify(exactly = 1) {
                        cmmnDeploymentBuilder.key(caseKey)
                        cmmnDeploymentBuilder.name(caseName)
                        cmmnDeploymentBuilder.addBytes(modelFileName, modelBytes)
                        cmmnDeploymentBuilder.deploy()
                    }
                }
            }
        }

        given("a CMMN model that has been deployed with the same content") {
            val cmmnRepositoryService = mockk<CmmnRepositoryService>()
            val cmmnDeployment = mockk<CmmnDeployment>()
            mockDeploymentQuery(cmmnRepositoryService, cmmnDeployment)
            val cmmnDeployer = CmmnDeployer(cmmnRepositoryService)
            every { cmmnDeployment.id } returns "fakeDeploymentId"
            every {
                cmmnRepositoryService.getResourceAsStream("fakeDeploymentId", modelFileName)
            } returns modelBytes.inputStream()

            `when`("the application starts") {
                cmmnDeployer.onStartup(Any())

                then("the model is not deployed again") {
                    verify(exactly = 0) { cmmnRepositoryService.createDeployment() }
                }
            }
        }

        given("a CMMN model that has been deployed with different content") {
            val cmmnRepositoryService = mockk<CmmnRepositoryService>()
            val cmmnDeployment = mockk<CmmnDeployment>()
            mockDeploymentQuery(cmmnRepositoryService, cmmnDeployment)
            val cmmnDeployer = CmmnDeployer(cmmnRepositoryService)
            every { cmmnDeployment.id } returns "fakeDeploymentId"
            every {
                cmmnRepositoryService.getResourceAsStream("fakeDeploymentId", modelFileName)
            } returns "fakeOtherModelContent".byteInputStream()
            val cmmnDeploymentBuilder = mockDeploymentBuilder(cmmnRepositoryService)

            `when`("the application starts") {
                cmmnDeployer.onStartup(Any())

                then("the new content of the model is deployed") {
                    verify(exactly = 1) {
                        cmmnDeploymentBuilder.addBytes(modelFileName, modelBytes)
                        cmmnDeploymentBuilder.deploy()
                    }
                }
            }
        }
    }
})
