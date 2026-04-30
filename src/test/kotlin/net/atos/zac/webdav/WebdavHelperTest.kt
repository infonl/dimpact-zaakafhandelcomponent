/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.webdav

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.core.UriBuilder
import jakarta.ws.rs.core.UriInfo
import net.atos.zac.util.MediaTypes
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import java.net.URI
import java.util.UUID

class WebdavHelperTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest

    val drcClientService = mockk<DrcClientService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()

    val webdavHelper = WebdavHelper()
    WebdavHelper::class.java.getDeclaredField("drcClientService").apply {
        isAccessible = true
        set(webdavHelper, drcClientService)
    }
    WebdavHelper::class.java.getDeclaredField("loggedInUserInstance").apply {
        isAccessible = true
        set(webdavHelper, loggedInUserInstance)
    }

    @Suppress("UNCHECKED_CAST")
    fun storedTokens(): Map<String, WebdavHelper.Gegevens> =
        WebdavHelper::class.java.getDeclaredField("tokenMap").apply {
            isAccessible = true
        }.get(webdavHelper) as Map<String, WebdavHelper.Gegevens>

    fun mockUriInfo(): Pair<UriInfo, UriBuilder> {
        val uriInfo = mockk<UriInfo>()
        val uriBuilder = mockk<UriBuilder>()
        every { uriInfo.baseUri } returns URI("http://localhost:8080/")
        every { uriInfo.baseUriBuilder } returns uriBuilder
        every { uriBuilder.replacePath(any()) } returns uriBuilder
        return uriInfo to uriBuilder
    }

    Given("a Word document (.doc format)") {
        val documentUUID = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            formaat = MediaTypes.Application.MS_WORD.mediaType
        ).apply { bestandsnaam("document.doc") }

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns document
        every { loggedInUserInstance.get() } returns loggedInUser

        val (uriInfo, uriBuilder) = mockUriInfo()
        val schemeSlot = slot<String>()
        every { uriBuilder.scheme(capture(schemeSlot)) } returns uriBuilder
        every { uriBuilder.build(*anyVararg<Any>()) } returns URI("ms-word:http://localhost:8080/webdav/folder/token.doc")

        When("createRedirectURL is called") {
            webdavHelper.createRedirectURL(documentUUID, uriInfo)

            Then("scheme uses ms-word protocol") {
                schemeSlot.captured shouldStartWith "ms-word:"
            }

            Then("token is stored and retrievable via readGegevens") {
                val token = storedTokens().keys.first()
                val gegevens = webdavHelper.readGegevens(token)
                gegevens.enkelvoudigInformatieibjectUUID shouldBe documentUUID
                gegevens.loggedInUser shouldBe loggedInUser
            }
        }
    }

    Given("a Word document (.docx OpenXML format)") {
        val documentUUID = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            formaat = MediaTypes.Application.MS_WORD_OPEN_XML.mediaType
        ).apply { bestandsnaam("document.docx") }

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns document
        every { loggedInUserInstance.get() } returns loggedInUser

        val (uriInfo, uriBuilder) = mockUriInfo()
        val schemeSlot = slot<String>()
        every { uriBuilder.scheme(capture(schemeSlot)) } returns uriBuilder
        every { uriBuilder.build(*anyVararg<Any>()) } returns URI("ms-word:http://localhost:8080/webdav/folder/token.docx")

        When("createRedirectURL is called") {
            webdavHelper.createRedirectURL(documentUUID, uriInfo)

            Then("scheme uses ms-word protocol") {
                schemeSlot.captured shouldStartWith "ms-word:"
            }
        }
    }

    Given("an Excel document (.xls format)") {
        val documentUUID = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            formaat = MediaTypes.Application.MS_EXCEL.mediaType
        ).apply { bestandsnaam("spreadsheet.xls") }

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns document
        every { loggedInUserInstance.get() } returns loggedInUser

        val (uriInfo, uriBuilder) = mockUriInfo()
        val schemeSlot = slot<String>()
        every { uriBuilder.scheme(capture(schemeSlot)) } returns uriBuilder
        every { uriBuilder.build(*anyVararg<Any>()) } returns URI("ms-excel:http://localhost:8080/webdav/folder/token.xls")

        When("createRedirectURL is called") {
            webdavHelper.createRedirectURL(documentUUID, uriInfo)

            Then("scheme uses ms-excel protocol") {
                schemeSlot.captured shouldStartWith "ms-excel:"
            }
        }
    }

    Given("an Excel document (.xlsx OpenXML format)") {
        val documentUUID = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            formaat = MediaTypes.Application.MS_EXCEL_OPEN_XML.mediaType
        ).apply { bestandsnaam("spreadsheet.xlsx") }

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns document
        every { loggedInUserInstance.get() } returns loggedInUser

        val (uriInfo, uriBuilder) = mockUriInfo()
        val schemeSlot = slot<String>()
        every { uriBuilder.scheme(capture(schemeSlot)) } returns uriBuilder
        every { uriBuilder.build(*anyVararg<Any>()) } returns URI("ms-excel:http://localhost:8080/webdav/folder/token.xlsx")

        When("createRedirectURL is called") {
            webdavHelper.createRedirectURL(documentUUID, uriInfo)

            Then("scheme uses ms-excel protocol") {
                schemeSlot.captured shouldStartWith "ms-excel:"
            }
        }
    }

    Given("a PowerPoint document (.pptx OpenXML format)") {
        val documentUUID = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            formaat = MediaTypes.Application.MS_POWER_POINT_OPEN_XML.mediaType
        ).apply { bestandsnaam("presentation.pptx") }

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns document
        every { loggedInUserInstance.get() } returns loggedInUser

        val (uriInfo, uriBuilder) = mockUriInfo()
        val schemeSlot = slot<String>()
        every { uriBuilder.scheme(capture(schemeSlot)) } returns uriBuilder
        every { uriBuilder.build(*anyVararg<Any>()) } returns URI("ms-powerpoint:http://localhost:8080/webdav/folder/token.pptx")

        When("createRedirectURL is called") {
            webdavHelper.createRedirectURL(documentUUID, uriInfo)

            Then("scheme uses ms-powerpoint protocol") {
                schemeSlot.captured shouldStartWith "ms-powerpoint:"
            }
        }
    }

    Given("a stored WebDAV token") {
        val documentUUID = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            uuid = documentUUID,
            formaat = MediaTypes.Application.MS_WORD_OPEN_XML.mediaType
        ).apply { bestandsnaam("document.docx") }

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns document
        every { loggedInUserInstance.get() } returns loggedInUser

        val (uriInfo, uriBuilder) = mockUriInfo()
        every { uriBuilder.scheme(any()) } returns uriBuilder
        every { uriBuilder.build(*anyVararg<Any>()) } returns URI("ms-word:http://localhost:8080/webdav/folder/token.docx")
        webdavHelper.createRedirectURL(documentUUID, uriInfo)

        When("readGegevens is called with the stored token") {
            val token = storedTokens().keys.first()
            val gegevens = webdavHelper.readGegevens(token)

            Then("it returns the correct Gegevens with the document UUID and logged-in user") {
                gegevens.enkelvoudigInformatieibjectUUID shouldBe documentUUID
                gegevens.loggedInUser shouldBe loggedInUser
            }
        }
    }

    Given("no token has been stored") {
        When("readGegevens is called with an unknown token") {
            Then("it throws a RuntimeException") {
                shouldThrow<RuntimeException> {
                    webdavHelper.readGegevens("nonexistent-token")
                }
            }
        }
    }
})
