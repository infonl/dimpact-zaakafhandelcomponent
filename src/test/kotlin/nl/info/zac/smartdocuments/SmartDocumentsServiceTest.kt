/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.ProcessingException
import nl.info.client.smartdocuments.SmartDocumentsClient
import nl.info.client.smartdocuments.model.createAttendedResponse
import nl.info.client.smartdocuments.model.createSmartDocument
import nl.info.client.smartdocuments.model.createsmartDocumentsTemplatesResponse
import nl.info.client.smartdocuments.model.document.OutputFormat
import nl.info.client.smartdocuments.model.document.Variables
import nl.info.client.smartdocuments.rest.DownloadedFile
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.documentcreation.model.DocumentCreationAttendedResponse
import nl.info.zac.documentcreation.model.createData
import nl.info.zac.log.EmployeeKeycloakUserIds
import nl.info.zac.log.LogCapture
import nl.info.zac.log.ZacLogger
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import nl.info.zac.smartdocuments.exception.SmartDocumentsUnsupportedOutputFormatException
import nl.info.zac.util.toBase64String
import java.net.URI
import java.util.Optional

class SmartDocumentsServiceTest : BehaviorSpec({
    val smartDocumentsURL = "https://example.com/fakeSmartDocumentsURL"
    val authenticationToken = "fakeAuthenticationToken"
    val fixedUserName = Optional.of("fakeFixedUserName")
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val smartDocumentsClient = mockk<Instance<SmartDocumentsClient>>()

    afterEach {
        checkUnnecessaryStub()
    }

    afterSpec {
        EmployeeKeycloakUserIds.reset()
    }

    given("SmartDocuments is enabled") {
        val data = createData()
        val variables = Variables(
            outputFormats = listOf(OutputFormat("DOCX")),
            redirectMethod = "POST",
            redirectUrl = "url"
        )
        val smartDocument = createSmartDocument(variables)
        val attendedResponse = createAttendedResponse()
        every {
            smartDocumentsClient.get().attendedDeposit(any(), fixedUserName.get(), any())
        } returns attendedResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("the 'create document attended' method is called") {
            lateinit var documentCreationResponse: DocumentCreationAttendedResponse
            val records = LogCapture(SmartDocumentsService::class).use { logCapture ->
                documentCreationResponse = smartDocumentsService.createDocumentAttended(
                    data = data,
                    smartDocument = smartDocument
                )
                logCapture.records
            }

            then(
                """
                the attended SmartDocuments document creation wizard is started and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    redirectUrl shouldBe URI(
                        "$smartDocumentsURL/smartdocuments/wizard?ticket=${attendedResponse.ticket}"
                    )
                    message shouldBe null
                }
            }

            then(
                "the document creation is logged as one successful record with its duration, with the fixed user " +
                    "name in use and without an employee"
            ) {
                with(records.single()) {
                    message shouldBe "SmartDocuments attended document creation completed"
                    mdc["outcome"] shouldBe "SUCCESS"
                    mdc.getValue("durationMs").toLong() shouldBeGreaterThanOrEqualTo 0L
                    mdc["errorType"] shouldBe "null"
                    mdc["isWizardAuthEnabled"] shouldBe "true"
                    mdc["isFixedUserName"] shouldBe "true"
                    mdc shouldNotContainKey "employee"
                    containsAnywhere(fixedUserName.get()) shouldBe false
                }
            }
        }
    }

    given("SmartDocuments is enabled and the SmartDocuments call fails") {
        val variables = Variables(outputFormats = listOf(OutputFormat("DOCX")), redirectMethod = "POST", redirectUrl = "url")
        val failure = ProcessingException("fakeSmartDocumentsFailure")
        every { smartDocumentsClient.get().attendedDeposit(any(), fixedUserName.get(), any()) } throws failure
        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("the 'create document attended' method is called") {
            lateinit var thrownException: ProcessingException
            val records = LogCapture(SmartDocumentsService::class).use { logCapture ->
                thrownException = shouldThrow<ProcessingException> {
                    smartDocumentsService.createDocumentAttended(data = createData(), smartDocument = createSmartDocument(variables))
                }
                logCapture.records
            }

            then("the failure is thrown to the caller") {
                thrownException shouldBe failure
            }

            then("the document creation is logged as one failed record with the error type, without the exception") {
                with(records.single()) {
                    mdc["outcome"] shouldBe "FAILURE"
                    mdc["errorType"] shouldBe ProcessingException::class.java.name
                    thrown shouldBe null
                    containsAnywhere("fakeSmartDocumentsFailure") shouldBe false
                }
            }
        }
    }

    given("SmartDocuments is enabled, no fixed user name is configured and an employee is logged in") {
        val loggedInUser = createLoggedInUser(username = "fakeSmartDocumentsUsername")
        val attendedResponse = createAttendedResponse()
        every { loggedInUserInstance.isUnsatisfied } returns false
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            smartDocumentsClient.get().attendedDeposit(any(), loggedInUser.username, any())
        } returns attendedResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = Optional.empty()
        )

        `when`("the 'create document attended' method is called in a request of that employee") {
            val records = LogCapture(SmartDocumentsService::class).use { logCapture ->
                ZacLogger.withLoggedInEmployee("fakeKeycloakUserId") {
                    smartDocumentsService.createDocumentAttended(
                        data = createData(),
                        smartDocument = createSmartDocument(
                            Variables(outputFormats = listOf(OutputFormat("DOCX")), redirectMethod = "POST", redirectUrl = "url")
                        )
                    )
                }
                logCapture.records
            }

            then("the document creation is logged with the Keycloak user ID of the request and not the username") {
                with(records.single()) {
                    mdc["isFixedUserName"] shouldBe "false"
                    mdc["employee"] shouldBe "fakeKeycloakUserId"
                    containsAnywhere(loggedInUser.username) shouldBe false
                }
            }
        }
    }

    listOf(
        "abcd.docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "abcd.pdf" to "application/pdf",
        "abcd.odt" to "application/vnd.oasis.opendocument.text"
    ).forEach { (generatedFileName, expectedOutputFormat) ->
        given(
            "SmartDocuments is enabled and a document named '$generatedFileName' is generated and ready for download"
        ) {
            val smartDocumentId = "sdId"
            val downloadedFile = mockk<DownloadedFile>()
            val body = "body content".toByteArray(Charsets.UTF_8)

            every { smartDocumentsClient.get().downloadFile(smartDocumentId, null) } returns downloadedFile
            every { downloadedFile.body() } returns body
            every { downloadedFile.contentDisposition() } returns "attachment; filename=\"$generatedFileName\""

            val smartDocumentsService = SmartDocumentsService(
                smartDocumentsClient = smartDocumentsClient,
                enabled = Optional.of(true),
                smartDocumentsURL = Optional.of(smartDocumentsURL),
                authenticationToken = Optional.of(authenticationToken),
                loggedInUserInstance = loggedInUserInstance,
                fixedUserName = fixedUserName
            )

            `when`("the 'download file' method is called") {
                val file = smartDocumentsService.downloadDocument(smartDocumentId)

                then("no output format is requested and the file's format is derived from its extension") {
                    with(file) {
                        fileName shouldBe generatedFileName
                        outputFormat shouldBe expectedOutputFormat
                        document.data shouldBe body.toBase64String()
                    }
                    verify(exactly = 1) { smartDocumentsClient.get().downloadFile(smartDocumentId, null) }
                }
            }
        }
    }

    listOf("abcd.xml", "abcd.html").forEach { fileName ->
        given("SmartDocuments is enabled and a downloaded document named '$fileName' has an unsupported format") {
            val smartDocumentId = "sdId"
            val downloadedFile = mockk<DownloadedFile>()

            every { smartDocumentsClient.get().downloadFile(smartDocumentId, null) } returns downloadedFile
            every { downloadedFile.contentDisposition() } returns "attachment; filename=\"$fileName\""
            every { downloadedFile.body() } returns "body content".toByteArray(Charsets.UTF_8)

            val smartDocumentsService = SmartDocumentsService(
                smartDocumentsClient = smartDocumentsClient,
                enabled = Optional.of(true),
                smartDocumentsURL = Optional.of(smartDocumentsURL),
                authenticationToken = Optional.of(authenticationToken),
                loggedInUserInstance = loggedInUserInstance,
                fixedUserName = fixedUserName
            )

            `when`("the 'download file' method is called") {
                val exception = shouldThrow<SmartDocumentsUnsupportedOutputFormatException> {
                    smartDocumentsService.downloadDocument(smartDocumentId)
                }

                then("it fails with an error identifying the unsupported extension and file name") {
                    exception.message shouldBe
                        "Unsupported SmartDocuments output file extension: '.${fileName.substringAfterLast('.')}' " +
                        "for file name: '$fileName'"
                }
            }
        }
    }

    given("SmartDocuments is enabled and contains templates") {
        val templatesResponse = createsmartDocumentsTemplatesResponse()
        every {
            smartDocumentsClient.get().listTemplates(any(), fixedUserName.get())
        } returns templatesResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("list templates is called") {
            val templatesList = smartDocumentsService.listTemplates()

            then("it should return a list of templates") {
                with(templatesList.documentsStructure.templatesStructure.templateGroups) {
                    size shouldBe 1
                    with(first()) {
                        name shouldBe "Dimpact"
                        templateGroups!!.size shouldBe 2
                        templateGroups!!.first().name shouldBe "Intern zaaktype voor test volledig gebruik ZAC"
                        templates!!.size shouldBe 2
                        templates!!.first().name shouldBe "Aanvullende informatie nieuw"
                    }
                }
            }
        }
    }

    given("SmartDocuments is enabled, no fixed user name is configured and a user is logged in") {
        val loggedInUser = createLoggedInUser()
        val templatesResponse = createsmartDocumentsTemplatesResponse()
        every { loggedInUserInstance.isUnsatisfied } returns false
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            smartDocumentsClient.get().listTemplates(any(), loggedInUser.username)
        } returns templatesResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = Optional.empty()
        )

        `when`("list templates is called") {
            val templatesList = smartDocumentsService.listTemplates()

            then("it requests templates for the logged-in user") {
                templatesList shouldBe templatesResponse
            }
        }
    }

    given("SmartDocuments is enabled, no fixed user name is configured and no user is logged in") {
        every { loggedInUserInstance.isUnsatisfied } returns true

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = Optional.empty()
        )

        `when`("list templates is called") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                smartDocumentsService.listTemplates()
            }

            then("it throws an exception") {
                exception.message shouldBe
                    "No SmartDocuments fixed user name configured and no user is currently logged in"
            }
        }
    }

    given("SmartDocuments is disabled") {
        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(false),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("checking if enabled") {
            then("it returns `false`") {
                smartDocumentsService.isEnabled() shouldBe false
            }
        }
    }

    given("SmartDocuments state is not specified") {
        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("checking if enabled") {
            then("it returns `false`") {
                smartDocumentsService.isEnabled() shouldBe false
            }
        }
    }

    given("SmartDocuments is enabled, but not enough configuration is provided") {
        `when`("SmartDocumentsService is constructed") {
            val exception = shouldThrow<IllegalArgumentException> {
                SmartDocumentsService(
                    smartDocumentsClient = smartDocumentsClient,
                    enabled = Optional.of(true),
                    loggedInUserInstance = loggedInUserInstance,
                    fixedUserName = fixedUserName
                )
            }

            then("it throws an exception") {
                exception.message shouldBe "SMARTDOCUMENTS_CLIENT_MP_REST_URL environment variable required"
            }
        }
    }

    given("SmartDocuments is enabled and wizard authentication is disabled") {
        val data = createData()
        val variables = Variables(
            outputFormats = listOf(OutputFormat("DOCX")),
            redirectMethod = "POST",
            redirectUrl = "url"
        )
        val smartDocument = createSmartDocument(variables)
        val attendedResponse = createAttendedResponse()

        every {
            smartDocumentsClient.get().attendedDepositNoAuth(any(), any())
        } returns attendedResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName,
            wizardAuthEnabled = Optional.of(false)
        )

        `when`("the 'createDocumentAttended' method is called without authorisation") {
            lateinit var response: DocumentCreationAttendedResponse
            val records = LogCapture(SmartDocumentsService::class).use { logCapture ->
                response = smartDocumentsService.createDocumentAttended(
                    data = data,
                    smartDocument = smartDocument
                )
                logCapture.records
            }

            then("it calls the attendedDepositNoAuth and returns the response") {
                with(response) {
                    redirectUrl shouldBe URI(
                        "$smartDocumentsURL/smartdocuments/wizard?ticket=${attendedResponse.ticket}"
                    )
                    message shouldBe null
                }
            }

            then("the document creation is logged without wizard authentication and without an employee") {
                with(records.single()) {
                    mdc["isWizardAuthEnabled"] shouldBe "false"
                    mdc shouldNotContainKey "employee"
                }
            }
        }
    }
})
