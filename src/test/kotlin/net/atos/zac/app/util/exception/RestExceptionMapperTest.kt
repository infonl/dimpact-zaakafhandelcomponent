/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.util.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import nl.info.client.bag.BagClientService
import nl.info.client.brp.exception.BrpTemporaryPersonIdNotCachedException
import nl.info.client.klant.KlantClientService
import nl.info.client.or.`object`.ObjectsClientService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.exception.BrcRuntimeException
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.shared.model.createFieldValidationError
import nl.info.client.zgw.shared.model.createValidationZgwError
import nl.info.client.zgw.zrc.exception.ZaakGeometrieNotSupportedException
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.exception.CatalogusNotFoundException
import nl.info.client.zgw.ztc.exception.ZtcRuntimeException
import nl.info.zac.admin.exception.SystemReferenceTableNotConfiguredException
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.AFZENDER
import nl.info.zac.app.exception.RestExceptionMapper
import nl.info.zac.app.klant.exception.RechtspersoonNotFoundException
import nl.info.zac.besluit.BesluitPublicationDateMissingException
import nl.info.zac.besluit.BesluitPublicationDisabledException
import nl.info.zac.besluit.BesluitResponseDateInvalidException
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BAG_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.exception.ServerErrorException
import nl.info.zac.exception.ZacSetupException
import nl.info.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.zac.log.LogCapture
import nl.info.zac.mailtemplates.exception.MailTemplateNotConfiguredException
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.policy.exception.OpaRuleNotConfiguredException
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import nl.info.zac.smartdocuments.exception.SmartDocumentsDisabledException
import nl.info.zac.zaak.exception.ZaakCannotBeSuspendedAgainException
import org.apache.http.HttpHost
import org.apache.http.HttpStatus
import org.apache.http.conn.HttpHostConnectException
import org.json.JSONObject
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.UnknownHostException
import java.util.logging.Level

@Suppress("LargeClass")
class RestExceptionMapperTest : BehaviorSpec({
    val restExceptionMapper = RestExceptionMapper()

    val logCapture = LogCapture(RestExceptionMapper::class)

    fun shouldHaveLoggedLast(level: Level, throwable: Throwable?, vararg fields: Pair<String, String>) =
        with(logCapture.records.last()) {
            this.level shouldBe level
            thrown shouldBe throwable
            mdc shouldContainAll fields.toMap()
        }

    afterSpec {
        logCapture.close()
    }

    context("Converting an exception to a JAX-RS response") {
        given(
            """
            A WebApplicationException with a status different from 500 and with as cause an IllegalArgumentException
            """
        ) {
            val exceptionMessage = "FakeRuntimeException"
            val exception = WebApplicationException(
                exceptionMessage,
                IllegalArgumentException(exceptionMessage),
                Response.Status.BAD_REQUEST
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return a specific error code and the exception message") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.invalid.argument",
                        exceptionMessage = exceptionMessage,
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                }

                and("it should log the error type of the exception at the level FINE, without the exception") {
                    shouldHaveLoggedLast(Level.FINE, null, "errorType" to exception.javaClass.name)
                }
            }
        }

        given("A WebApplicationException with a status different from 500 and no message") {
            val exception = WebApplicationException(null as String?, Response.Status.NOT_FOUND)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the generic server error code as the message and the not found status") {
                    checkResponse(response, "msg.error.server.generic", expectedStatus = HttpStatus.SC_NOT_FOUND)
                }

                and("it should log a fallback message naming the response status, at the level FINE") {
                    shouldHaveLoggedLast(Level.FINE, null, "errorType" to exception.javaClass.name, "httpStatus" to "404")
                }
            }
        }

        given("A not found exception whose message holds the KVK number of the request") {
            val exception = RechtspersoonNotFoundException("Geen rechtspersoon gevonden voor KVK nummer '12345678'")

            `when`("the exception is mapped to a response") {
                restExceptionMapper.toResponse(exception)

                then("the KVK number is nowhere in the log record, which has only the error type") {
                    with(logCapture.records.last()) {
                        containsAnywhere("12345678") shouldBe false
                        mdc["errorType"] shouldBe RechtspersoonNotFoundException::class.java.name
                    }
                }
            }
        }

        given("A runtime exception") {
            val exceptionMessage = "FakeRuntimeException"
            val exception = RuntimeException(exceptionMessage)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the generic server error code and the exception message and log the exception") {
                    checkResponse(response, "msg.error.server.generic", exceptionMessage)
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A BRC runtime exception") {
            val exceptionMessage = "FakeRuntimeException"
            val exception = BrcRuntimeException(exceptionMessage)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the BRC server error code and log the exception") {
                    checkResponse(response, "msg.error.brc.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A DRC runtime exception") {
            val exceptionMessage = "FakeRuntimeException"
            val exception = DrcRuntimeException(exceptionMessage)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the DRC server error code and log the exception") {
                    checkResponse(response, "msg.error.drc.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A ZRC runtime exception") {
            val exceptionMessage = "FakeRuntimeException"
            val exception = ZrcRuntimeException(exceptionMessage)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the ZRC server error code and log the exception") {
                    checkResponse(response, "msg.error.zrc.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A ZaakGeometrieNotSupportedException exception") {
            val exceptionMessage = "Zaak 'fakeZaakUUID' has an unsupported zaakgeometrie type. " +
                "Only 'Point' zaakgeometrie is supported."
            val exception = ZaakGeometrieNotSupportedException(exceptionMessage, RuntimeException("cause"))

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the zaak geometrie not supported error code, a bad request status, and log at WARNING") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.zaak.geometrie.not-supported",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    shouldHaveLoggedLast(Level.WARNING, null, "errorType" to exception.javaClass.name)
                }
            }
        }

        given("A ZaakCannotBeSuspendedAgainException exception") {
            val zaakCannotBeSuspendedAgainException = ZaakCannotBeSuspendedAgainException(
                "Zaak 'fakeZaakIdentificatie' was suspended before. A zaak can be suspended only one time."
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(zaakCannotBeSuspendedAgainException)

                then("it returns the zaak cannot be suspended again error code and a conflict status, and logs at FINE") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.zaak.cannot.be.suspended.again",
                        expectedStatus = HttpStatus.SC_CONFLICT
                    )
                    shouldHaveLoggedLast(Level.FINE, null, "errorType" to zaakCannotBeSuspendedAgainException.javaClass.name)
                }
            }
        }

        given("A ZGW runtime exception") {
            val exceptionMessage = "FakeRuntimeException"
            val exception = ZgwRuntimeException(exceptionMessage)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the generic server error code and the exception message and log the exception") {
                    checkResponse(response, "msg.error.server.generic", exceptionMessage)
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A ZTC runtime exception") {
            val exceptionMessage = "FakeRuntimeException"
            val exception = ZtcRuntimeException(exceptionMessage)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the ZTC server error code and the exception message and log the exception") {
                    checkResponse(response, "msg.error.ztc.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A BrpTemporaryPersonIdNotCachedException exception") {
            val exception = BrpTemporaryPersonIdNotCachedException("Geen persoon gevonden voor id 'fakeUuid'")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the temporary person id expired error code, no exception message, and a Gone status") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.brp.temporary.person.id.expired",
                        expectedStatus = HttpStatus.SC_GONE
                    )
                    shouldHaveLoggedLast(Level.FINE, null, "errorType" to exception.javaClass.name)
                }
            }
        }

        given("A ZGW validation error exception with two invalid parameters") {
            val zgwValidationErrorException = ZgwValidationErrorException(
                createValidationZgwError(
                    code = "fakeCode",
                    title = "fakeTitle",
                    status = 12345,
                    detail = "fakeDetail",
                    invalidParams = listOf(
                        createFieldValidationError(
                            name = "fakeFieldName1",
                            code = "fakeCode1",
                            reason = "fakeReason1"
                        ),
                        createFieldValidationError(
                            name = "fakeFieldName2",
                            code = "fakeCode2",
                            reason = "fakeReason2"
                        )
                    )
                )
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(zgwValidationErrorException)

                then(
                    """
                it should return the zgw validation error code and the exception message consisting of the reasons of the 
                field validation errors separated by a comma
                """
                ) {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.validation.zgw",
                        exceptionMessage = "fakeReason1, fakeReason2",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                }
                and("it should log the error code and the names and codes of the invalid parameters at FINE, without the reasons") {
                    shouldHaveLoggedLast(
                        Level.FINE,
                        null,
                        "event" to "ZgwValidationFailed",
                        "errorCode" to "fakeCode",
                        "invalidParams" to "[fakeFieldName1:fakeCode1, fakeFieldName2:fakeCode2]"
                    )
                    logCapture.records.last().containsAnywhere("fakeReason") shouldBe false
                    logCapture.records.last().containsAnywhere("fakeDetail") shouldBe false
                }
            }
        }

        given(
            "A Flowable ZGW validation error exception with a ZGW validation error exception without invalid parameters as cause"
        ) {
            val zgwValidationErrorException = ZgwValidationErrorException(
                createValidationZgwError(
                    code = "fakeCode",
                    title = "fakeTitle",
                    status = 12345,
                    detail = "fakeDetail",
                    invalidParams = emptyList()
                )
            )
            val flowableZgwValidationErrorException = FlowableZgwValidationErrorException(
                message = "fakeMessage",
                cause = zgwValidationErrorException
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(flowableZgwValidationErrorException)

                then(
                    """
                it should return the zgw validation error code and the exception message
                """
                ) {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.validation.zgw",
                        exceptionMessage = "",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                }
                and("it should log the error code of the root cause at FINE, without the exception") {
                    shouldHaveLoggedLast(Level.FINE, null, "errorCode" to "fakeCode", "invalidParams" to "[]")
                    logCapture.records.last().containsAnywhere("fakeDetail") shouldBe false
                }
            }
        }

        given(
            """
        A JAX-RS processing exception with as root cause a HttpHostConnectException
        and which contains the BAG client service class name in the stacktrace
        """
        ) {
            val exceptionMessage = "FakeProcessingException"
            val exception = ProcessingException(
                exceptionMessage,
                HttpHostConnectException(
                    IOException("Something terrible happened in the ${BagClientService::class.simpleName}!"),
                    HttpHost("localhost", 8080)
                )
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the BAG server error code and no exception message and log the exception") {
                    checkResponse(response, "msg.error.bag.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given(
            """
        A JAX-RS processing exception with as root cause a HttpHostConnectException
        and which contains the BRC client service class name in the stacktrace
        """
        ) {
            val exceptionMessage = "FakeProcessingException"
            val exception = ProcessingException(
                exceptionMessage,
                HttpHostConnectException(
                    IOException("Something terrible happened in the ${BrcClientService::class.simpleName}!"),
                    HttpHost("localhost", 8080)
                )
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the BRC server error code and no exception message and log the exception") {
                    checkResponse(response, "msg.error.brc.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given(
            """
        A JAX-RS processing exception with as root cause a HttpHostConnectException
        and which contains the Klanten client service class name in the stacktrace
        """
        ) {
            val exceptionMessage = "FakeProcessingException"
            val exception = ProcessingException(
                exceptionMessage,
                HttpHostConnectException(
                    IOException("Something terrible happened in the ${KlantClientService::class.simpleName}!"),
                    HttpHost("localhost", 8080)
                )
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the Klanten server error code and no exception message and log the exception") {
                    checkResponse(response, "msg.error.klanten.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given(
            """
        A JAX-RS processing exception with as root cause a HttpHostConnectException
        and which contains the Objecten client service class name in the stacktrace
        """
        ) {
            val exceptionMessage = "FakeProcessingException"
            val exception = ProcessingException(
                exceptionMessage,
                HttpHostConnectException(
                    IOException("Something terrible happened in the ${ObjectsClientService::class.simpleName}!"),
                    HttpHost("localhost", 8080)
                )
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the Objecten server error code and no exception message and log the exception") {
                    checkResponse(response, "msg.error.objects.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given(
            """
        A JAX-RS processing exception with as root cause a UnknownHostException
        which contains the ZTC client service class name in the stacktrace
        """
        ) {
            val exceptionMessage = "FakeProcessingException"
            val exception = ProcessingException(
                exceptionMessage,
                UnknownHostException("Something terrible happened in the ${ZtcClientService::class.simpleName}!")
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the ZTC server error code and no exception message and log the exception") {
                    checkResponse(response, "msg.error.ztc.client.exception")
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given(
            """
        A JAX-RS processing exception without a HttpHostConnectException or UnknownHostException
        as cause but which does contain a mapped client service class name in the stacktrace
        """
        ) {
            val exceptionMessage = "FakeProcessingException"
            val exception = ProcessingException(
                exceptionMessage,
                RuntimeException("Something terrible happened in the ${ZtcClientService::class.simpleName}!")
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then(
                    "it should return the general server error error code with an exception message and log the exception"
                ) {
                    checkResponse(response, "msg.error.server.generic", exceptionMessage)
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("An input validation exception with an error code and a message") {
            val exception = InputValidationFailedException(
                errorCode = ERROR_CODE_BAG_CLIENT,
                message = "fakeErrorMessage"
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then(
                    """
                    it should return a bad request status with error message equal to the value of the
                    error code and it should log the exception including the error message
                """
                ) {
                    checkResponse(
                        response = response,
                        errorMessage = ERROR_CODE_BAG_CLIENT.value,
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    shouldHaveLoggedLast(Level.FINE, null, "errorType" to exception.javaClass.name)
                }
            }
        }

        given("An input validation exception with an error code and no message") {
            val exception = InputValidationFailedException(
                errorCode = ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
            )

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then(
                    """
                    it should return a bad request status with error message equal to the value of the
                    error code and it should log the exception including a generic error message
                """
                ) {
                    checkResponse(
                        response = response,
                        errorMessage = ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS.value,
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    shouldHaveLoggedLast(
                        Level.FINE,
                        null,
                        "errorType" to exception.javaClass.name,
                        "errorCode" to ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS.value
                    )
                }
            }
        }

        given("A BesluitPublicationDisabledException exception") {
            val exception = BesluitPublicationDisabledException("error")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the proper error code and no exception message, and log the error type without the exception") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.besluit.publication.disabled",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    shouldHaveLoggedLast(Level.FINE, null, "errorType" to exception.javaClass.name)
                }
            }
        }

        given("A BesluitPublicationDateMissingException exception") {
            val exception = BesluitPublicationDateMissingException()

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the proper error code and no exception message, and log the error type without the exception") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.besluit.publication.date.missing",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    shouldHaveLoggedLast(
                        Level.FINE,
                        null,
                        "errorType" to exception.javaClass.name,
                        "errorCode" to "msg.error.besluit.publication.date.missing"
                    )
                }
            }
        }

        given("A BesluitResponseDateInvalidException exception") {
            val exception = BesluitResponseDateInvalidException("error")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the proper error code and no exception message, and log the error type without the exception") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.besluit.response.date.invalid",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    shouldHaveLoggedLast(Level.FINE, null, "errorType" to exception.javaClass.name)
                }
            }
        }
        given("A server error exception") {
            val errorCode = mockk<ErrorCode>()
            val exception = ServerErrorException(errorCode, "error")
            every { errorCode.value } returns "fakeErrorCodeValue"

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the expected error code and no exception message and log the exception") {
                    checkResponse(
                        response = response,
                        errorMessage = "fakeErrorCodeValue",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A SmartDocumentsConfigurationException exception") {
            val exception = SmartDocumentsConfigurationException("fakeMessage")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return a server error status with the SmartDocuments not configured error code") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.smartdocuments.not.configured",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                }

                and("it should log the exception at the level SEVERE") {
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A SmartDocumentsDisabledException exception") {
            val exception = SmartDocumentsDisabledException()

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return a server error status with the SmartDocuments disabled error code") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.smartdocuments.disabled",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                }

                and("it should log the exception at the level SEVERE") {
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A SystemReferenceTableNotConfiguredException exception") {
            val exception = SystemReferenceTableNotConfiguredException(AFZENDER)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return a server error status with the system reference table not configured error code") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.system.reference.table.not.configured",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                }

                and("it should log the exception at the level SEVERE") {
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A MailTemplateNotConfiguredException exception") {
            val exception = MailTemplateNotConfiguredException(Mail.ZAAK_ONTVANKELIJK)

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return a server error status with the mail template not configured error code") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.mailtemplate.not.configured",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                }

                and("it should log the exception at the level SEVERE") {
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A CatalogusNotFoundException exception") {
            val exception = CatalogusNotFoundException("fakeMessage")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return a server error status with the catalogus not configured error code") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.catalogus.not.configured",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                }

                and("it should log the exception at the level SEVERE") {
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("An OpaRuleNotConfiguredException exception") {
            val exception = OpaRuleNotConfiguredException("zaak/zaak_rechten")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return a server error status with the OPA rule not configured error code") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.opa.rule.not.configured",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                }

                and("it should log the exception at the level SEVERE") {
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("An exception chain IllegalArgumentException->InvocationTargetException->ServerErrorException") {
            val exception = ServerErrorException(ErrorCode.ERROR_CODE_BAD_BRP_PROTOCOLLERING_CONFIGURATION, "message")
            val chain = IllegalArgumentException(InvocationTargetException(exception))

            `when`("the chain is mapped to a response") {
                val response = restExceptionMapper.toResponse(chain)

                then("it should return the expected error code and exception message") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.bad.brp.protocollering.configuration",
                        exceptionMessage = "message",
                        expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                    )
                }

                and("it should log the exception") {
                    shouldHaveLoggedLast(Level.SEVERE, exception)
                }
            }
        }

        given("A ZAC setup exception") {
            val exception = ZacSetupException("fakeMessage", ErrorCode.ERROR_CODE_BPMN_TASK_FORM_NOT_FOUND)

            `when`("the chain is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the expected error code and exception message") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.bpmn.task.form.not.found",
                        exceptionMessage = "fakeMessage",
                        expectedStatus = HttpStatus.SC_CONFLICT
                    )
                }

                and("it should log the error type without the exception") {
                    shouldHaveLoggedLast(Level.WARNING, null, "errorType" to exception.javaClass.name)
                }
            }
        }
    }
})

fun checkResponse(
    response: Response,
    errorMessage: String,
    exceptionMessage: String? = null,
    expectedStatus: Int = HttpStatus.SC_INTERNAL_SERVER_ERROR
): Unit =
    with(response) {
        mediaType shouldBe MediaType.APPLICATION_JSON_TYPE
        status shouldBe expectedStatus
        val entityAsJson = JSONObject(readEntity(String::class.java))
        with(entityAsJson) {
            getString("message") shouldBe errorMessage
            exceptionMessage?.let {
                getString("exception") shouldBe exceptionMessage
            } ?: (has("exception") shouldBe false)
        }
    }
