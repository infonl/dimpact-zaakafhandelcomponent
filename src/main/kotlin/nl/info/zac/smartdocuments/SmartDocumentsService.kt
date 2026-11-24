/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import nl.info.client.smartdocuments.SmartDocumentsClient
import nl.info.client.smartdocuments.model.document.Data
import nl.info.client.smartdocuments.model.document.Deposit
import nl.info.client.smartdocuments.model.document.Document
import nl.info.client.smartdocuments.model.document.File
import nl.info.client.smartdocuments.model.document.SmartDocument
import nl.info.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.documentcreation.model.DocumentCreationAttendedResponse
import nl.info.zac.log.Employee
import nl.info.zac.log.Outcome
import nl.info.zac.log.ZacLogger
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import nl.info.zac.smartdocuments.exception.SmartDocumentsUnsupportedOutputFormatException
import nl.info.zac.smartdocuments.log.SmartDocumentsAttendedDepositCompleted
import nl.info.zac.util.AllOpen
import nl.info.zac.util.MediaTypes
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.toBase64String
import org.apache.commons.io.FilenameUtils.getExtension
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.Optional
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrDefault

@NoArgConstructor
@ApplicationScoped
@AllOpen
@Suppress("LongParameterList")
class SmartDocumentsService @Inject constructor(
    // RestEasy declarative clients use configuration properties
    // To make the client optional we use Instance, which is an alternative to @Autowire(required=false) in Spring
    @RestClient
    private val smartDocumentsClient: Instance<SmartDocumentsClient>,

    // With nullable Kotlin types ConfigProperty and Weld error with:
    //     io.smallrye.config.inject.ConfigException: SRCFG02000: Failed to Inject @ConfigProperty for key,
    // Therefore, we use Optional to support non-mandatory properties.
    // Weld injects Optional.empty() if a property is not available and overrides the Kotlin default value.

    @ConfigProperty(name = "SMARTDOCUMENTS_ENABLED")
    private val enabled: Optional<Boolean> = Optional.empty(),

    @ConfigProperty(name = "SMARTDOCUMENTS_CLIENT_MP_REST_URL")
    private val smartDocumentsURL: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "SMARTDOCUMENTS_AUTHENTICATION")
    private val authenticationToken: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "SMARTDOCUMENTS_FIXED_USER_NAME")
    private val fixedUserName: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "SMARTDOCUMENTS_WIZARD_AUTH_ENABLED")
    private val wizardAuthEnabled: Optional<Boolean> = Optional.empty(),

    private val loggedInUserInstance: Instance<LoggedInUser>,
) {
    init {
        if (isEnabled()) {
            require(smartDocumentsURL.isPresent) { "SMARTDOCUMENTS_CLIENT_MP_REST_URL environment variable required" }
            require(authenticationToken.isPresent) { "SMARTDOCUMENTS_AUTHENTICATION environment variable required" }
        }
    }

    companion object {
        private val LOG = ZacLogger.getLogger(SmartDocumentsService::class)
    }

    fun isEnabled() = enabled.getOrDefault(false)
    fun useWizardAuthEnabled() = wizardAuthEnabled.getOrDefault(true)

    /**
     * Sends a request to SmartDocuments to create a document using the Smart Documents wizard (= attended mode).
     * A failure of the request is rethrown; it is only caught to log its type.
     */
    @Suppress("TooGenericExceptionCaught")
    fun createDocumentAttended(
        data: Data,
        smartDocument: SmartDocument
    ): DocumentCreationAttendedResponse {
        val deposit = Deposit(
            data = data,
            smartDocument = smartDocument
        )
        val isWizardAuthEnabled = useWizardAuthEnabled()
        val startTime = System.nanoTime()
        var outcome = Outcome.FAILURE
        var errorType: String? = null
        val attendedResponse = try {
            if (isWizardAuthEnabled) {
                smartDocumentsClient.get().attendedDeposit(
                    authenticationToken = "Basic ${authenticationToken.get()}",
                    userName = determineUserName(),
                    deposit = deposit
                )
            } else {
                smartDocumentsClient.get().attendedDepositNoAuth(
                    authenticationToken = "Basic ${authenticationToken.get()}",
                    deposit = deposit
                )
            }.also { outcome = Outcome.SUCCESS }
        } catch (runtimeException: RuntimeException) {
            errorType = runtimeException.javaClass.name
            throw runtimeException
        } finally {
            LOG.fine {
                SmartDocumentsAttendedDepositCompleted(
                    isWizardAuthEnabled = isWizardAuthEnabled,
                    isFixedUserName = fixedUserName.isPresent,
                    employee = if (isWizardAuthEnabled && !fixedUserName.isPresent) loggedInEmployee() else null,
                    outcome = outcome,
                    durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime),
                    errorType = errorType
                )
            }
        }
        return DocumentCreationAttendedResponse(
            redirectUrl = UriBuilder.fromUri(smartDocumentsURL.get())
                .path("smartdocuments/wizard")
                .queryParam("ticket", attendedResponse.ticket)
                .build()
        )
    }

    /**
     * Lists all SmartDocuments templates groups and templates available for the current user.
     *
     * @return A structure describing templates and groups
     */
    fun listTemplates(): SmartDocumentsTemplatesResponse {
        val userName = determineUserName()
        return smartDocumentsClient.get().listTemplates(
            authenticationToken = "Basic ${authenticationToken.get()}",
            userName = userName
        )
    }

    /**
     * Download the generated document from SmartDocuments.
     */
    fun downloadDocument(fileId: String): File =
        smartDocumentsClient.get().downloadFile(
            smartDocumentsId = fileId
        ).let { downloadedFile ->
            val fileName = downloadedFile.contentDisposition()
                .removePrefix("attachment; filename=\"")
                .removeSuffix("\"")
            File(
                fileName = fileName,
                document = Document(data = downloadedFile.body().toBase64String()),
                outputFormat = outputFormatForFileName(fileName)
            )
        }

    /**
     * Determines the username to use for SmartDocuments requests: the configured fixed username if present,
     * or else the currently logged-in user's id.
     */
    private fun loggedInEmployee() =
        loggedInUserInstance.takeUnless { it.isUnsatisfied }?.get()?.username?.let(::Employee)

    private fun determineUserName(): String =
        fixedUserName.orElseGet {
            if (loggedInUserInstance.isUnsatisfied) {
                throw SmartDocumentsConfigurationException(
                    "No SmartDocuments fixed user name configured and no user is currently logged in"
                )
            }
            loggedInUserInstance.get().username
        }

    private fun outputFormatForFileName(fileName: String): String =
        ".${getExtension(fileName)}".let { extension ->
            MediaTypes.Application.entries.find { extension in it.extensions }?.mediaType
                ?: throw SmartDocumentsUnsupportedOutputFormatException(
                    "Unsupported SmartDocuments output file extension: '$extension' for file name: '$fileName'"
                )
        }
}
