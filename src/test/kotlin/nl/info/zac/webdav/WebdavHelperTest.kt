/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.webdav

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.core.UriBuilder
import jakarta.ws.rs.core.UriInfo
import net.atos.zac.util.MediaTypes
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.webdav.exceptions.WebdavException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import java.net.URI
import java.util.UUID

class WebdavHelperTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()

    val webdavHelper = WebdavHelper(drcClientService, loggedInUserInstance)

    afterEach {
        checkUnnecessaryStub()
    }

    @Suppress("UNCHECKED_CAST")
    fun storedTokens(): Map<String, WebdavHelper.WebdavTokenData> =
        WebdavHelper::class.java.getDeclaredField("tokenMap").apply {
            isAccessible = true
        }.get(webdavHelper) as Map<String, WebdavHelper.WebdavTokenData>

    fun mockUriInfo(): Pair<UriInfo, UriBuilder> {
        val uriInfo = mockk<UriInfo>()
        val uriBuilder = mockk<UriBuilder>()
        every { uriInfo.baseUri } returns URI("http://localhost:8080/")
        every { uriInfo.baseUriBuilder } returns uriBuilder
        every { uriBuilder.replacePath(any()) } returns uriBuilder
        return uriInfo to uriBuilder
    }

    // In SingleInstance BehaviorSpec, Given/When bodies run once at collection time.
    // createRedirectURL is called there; schemeSlot and storedGegevens are captured via closure.
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

        val tokensBefore = storedTokens().keys.toSet()
        webdavHelper.createRedirectURL(documentUUID, uriInfo)
        val newToken = (storedTokens().keys.toSet() - tokensBefore).first()
        val storedGegevens = webdavHelper.readWebdavTokenData(newToken)

        When("createRedirectURL is called") {
            Then("scheme uses ms-word protocol") {
                schemeSlot.captured shouldStartWith "ms-word:"
            }

            Then("token is stored with the correct document UUID and logged-in user") {
                storedGegevens.enkelvoudigInformatieobjectUUID shouldBe documentUUID
                storedGegevens.loggedInUser shouldBe loggedInUser
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
        webdavHelper.createRedirectURL(documentUUID, uriInfo)

        When("createRedirectURL is called") {
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
        webdavHelper.createRedirectURL(documentUUID, uriInfo)

        When("createRedirectURL is called") {
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
        webdavHelper.createRedirectURL(documentUUID, uriInfo)

        When("createRedirectURL is called") {
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
        every {
            uriBuilder.build(*anyVararg<Any>())
        } returns URI("ms-powerpoint:http://localhost:8080/webdav/folder/token.pptx")
        webdavHelper.createRedirectURL(documentUUID, uriInfo)

        When("createRedirectURL is called") {
            Then("scheme uses ms-powerpoint protocol") {
                schemeSlot.captured shouldStartWith "ms-powerpoint:"
            }
        }
    }

    Given("no token has been stored") {
        When("readGegevens is called with an unknown token") {
            val webdavException = shouldThrow<WebdavException> {
                webdavHelper.readWebdavTokenData("nonexistent-token")
            }

            Then("it throws a WebdavException with the correct message") {
                webdavException.message shouldBe "WebDAV token does not exist (anymore)."
            }
        }
    }
})
