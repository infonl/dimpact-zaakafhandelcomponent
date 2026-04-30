/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.webdav

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.spi.CDI
import jakarta.servlet.http.HttpSession
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.setLoggedInUser
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

class WebdavStoreTest : BehaviorSpec({
    mockkStatic(::setLoggedInUser)
    val webdavHelper = mockk<WebdavHelper>()
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()

    fun setupCdi(
        webdavHelper: WebdavHelper,
        drcClientService: DrcClientService,
        enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
        httpSession: HttpSession? = null
    ) {
        mockkStatic(CDI::class)
        val cdi = mockk<CDI<Any>>()
        every { CDI.current() } returns cdi
        every { cdi.select(WebdavHelper::class.java) } returns mockk<Instance<WebdavHelper>>().also {
            every { it.get() } returns webdavHelper
        }
        every { cdi.select(DrcClientService::class.java) } returns mockk<Instance<DrcClientService>>().also {
            every { it.get() } returns drcClientService
        }
        every { cdi.select(EnkelvoudigInformatieObjectUpdateService::class.java) } returns
            mockk<Instance<EnkelvoudigInformatieObjectUpdateService>>().also {
                every { it.get() } returns enkelvoudigInformatieObjectUpdateService
            }
        if (httpSession != null) {
            every { cdi.select(HttpSession::class.java) } returns mockk<Instance<HttpSession>>().also {
                every { it.get() } returns httpSession
            }
        }
    }

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a valid WebDAV token for read operations") {
        setupCdi(webdavHelper, drcClientService, enkelvoudigInformatieObjectUpdateService)
        val webdavStore = WebdavStore(File("/fake"))
        val documentUUID = UUID.randomUUID()
        val token = UUID.randomUUID().toString()
        val gegevens = WebdavHelper.Gegevens(documentUUID, mockk<LoggedInUser>())

        every { webdavHelper.readGegevens(token) } returns gegevens

        When("getResourceContent is called with a valid token URI") {
            val inputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
            every { drcClientService.downloadEnkelvoudigInformatieobject(documentUUID) } returns inputStream

            val result = webdavStore.getResourceContent(null, "/webdav/folder/$token.docx")

            Then("it returns the InputStream downloaded from DRC") {
                result shouldBe inputStream
                verify(exactly = 1) { drcClientService.downloadEnkelvoudigInformatieobject(documentUUID) }
            }
        }

        When("getStoredObject is called with a valid token URI") {
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
                uuid = documentUUID,
                bestandsomvang = 5000
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject

            val result = webdavStore.getStoredObject(null, "/webdav/folder/$token.docx")

            Then("it returns a file StoredObject with the correct metadata") {
                result shouldNotBe null
                result!!.isFolder shouldBe false
                result.resourceLength shouldBe 5000L
            }
        }
    }

    Given("a valid WebDAV token for write operations") {
        val httpSession = mockk<HttpSession>()
        setupCdi(webdavHelper, drcClientService, enkelvoudigInformatieObjectUpdateService, httpSession)
        val webdavStore = WebdavStore(File("/fake"))

        val documentUUID = UUID.randomUUID()
        val token = UUID.randomUUID().toString()
        val loggedInUser = mockk<LoggedInUser>()
        val gegevens = WebdavHelper.Gegevens(documentUUID, loggedInUser)

        every { webdavHelper.readGegevens(token) } returns gegevens

        When("setResourceContent is called with a valid token URI and document content") {
            val contentStream = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5))
            val updatedDocument = createEnkelvoudigInformatieObject(
                uuid = documentUUID,
                bestandsomvang = 5
            )
            every { setLoggedInUser(httpSession, loggedInUser) } just runs
            every {
                enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                    documentUUID,
                    any(),
                    "Document bewerkt"
                )
            } returns updatedDocument

            val result = webdavStore.setResourceContent(
                null,
                "/webdav/folder/$token.docx",
                contentStream,
                null,
                null
            )

            Then("it updates the document in DRC and returns the updated file size") {
                result shouldBe 5L
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                        documentUUID,
                        any(),
                        "Document bewerkt"
                    )
                }
            }
        }
    }

    Given("a folder URI") {
        setupCdi(mockk(), mockk(), mockk())
        val webdavStore = WebdavStore(File("/fake"))

        When("getStoredObject is called") {
            val result = webdavStore.getStoredObject(null, "/webdav/folder")

            Then("it returns a folder StoredObject") {
                result shouldNotBe null
                result!!.isFolder shouldBe true
            }
        }
    }

    Given("a null URI") {
        setupCdi(mockk(), mockk(), mockk())
        val webdavStore = WebdavStore(File("/fake"))

        When("getResourceContent is called") {
            val result = webdavStore.getResourceContent(null, null)

            Then("it returns null") {
                result shouldBe null
            }
        }

        When("getStoredObject is called") {
            val result = webdavStore.getStoredObject(null, null)

            Then("it returns null") {
                result shouldBe null
            }
        }

        When("setResourceContent is called") {
            val result = webdavStore.setResourceContent(
                null,
                null,
                ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                null,
                null
            )

            Then("it returns 0") {
                result shouldBe 0L
            }
        }
    }

    Given("no URI-specific behavior expected") {
        setupCdi(mockk(), mockk(), mockk())
        val webdavStore = WebdavStore(File("/fake"))

        When("getChildrenNames is called") {
            val result = webdavStore.getChildrenNames(null, "/webdav/folder")

            Then("it returns null") {
                result shouldBe null
            }
        }

        When("getResourceLength is called") {
            val result = webdavStore.getResourceLength(null, "/webdav/folder/sometoken.docx")

            Then("it returns 0") {
                result shouldBe 0L
            }
        }
    }
})
