/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.util.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.client.zgw.shared.model.createFieldValidationError
import net.atos.client.zgw.shared.model.createValidationZgwError
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.client.bag.BagClientService
import nl.info.client.klant.KlantClientService
import nl.info.client.or.`object`.ObjectsClientService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.exception.BrcRuntimeException
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.exception.ZtcRuntimeException
import nl.info.zac.app.exception.RestExceptionMapper
import nl.info.zac.besluit.BesluitPublicationDateMissingException
import nl.info.zac.besluit.BesluitPublicationDisabledException
import nl.info.zac.besluit.BesluitResponseDateInvalidException
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BAG_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.exception.ServerErrorException
import nl.info.zac.exception.ZacSetupException
import nl.info.zac.log.log
import org.apache.http.HttpHost
import org.apache.http.HttpStatus
import org.apache.http.conn.HttpHostConnectException
import org.json.JSONObject
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.UnknownHostException
import java.util.logging.Level

class RestExceptionMapperTest : BehaviorSpec({
    val restExceptionMapper = RestExceptionMapper()

    beforeSpec {
        mockkStatic("nl.info.zac.log.LogUtilsKt")
    }

    afterSpec {
        unmockkStatic("nl.info.zac.log.LogUtilsKt")
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
                    checkResponse(response, "msg.error.invalid.argument", exceptionMessage, HttpStatus.SC_BAD_REQUEST)
                }

                And("it should not log the exception") {
                    verify(exactly = 0) { log(any(), any(), any<String>(), any()) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                And("it should log the exception at the level FINE with the expected exception message") {
                    verify(exactly = 1) {
                        log(
                            logger = any(),
                            level = Level.FINE,
                            message = "fakeTitle [12345 fakeCode] fakeDetail: fakeFieldName1 [fakeCode1] fakeReason1, " +
                                "fakeFieldName2 [fakeCode2] fakeReason2 " +
                                "(https://localhost:8080/validation-error https://localhost:8080/validation-error-instance)",
                            throwable = zgwValidationErrorException
                        )
                    }
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
                And("it should log the exception at the level FINE with the expected root cause exception message") {
                    verify(exactly = 1) {
                        log(
                            logger = any(),
                            level = Level.FINE,
                            message = "fakeTitle [12345 fakeCode] fakeDetail:  " +
                                "(https://localhost:8080/validation-error https://localhost:8080/validation-error-instance)",
                            throwable = zgwValidationErrorException
                        )
                    }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
                    verify(
                        exactly = 1
                    ) {
                        log(
                            any(),
                            Level.FINE,
                            "fakeErrorMessage",
                            exception
                        )
                    }
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
                    verify(exactly = 1) {
                        log(
                            any(),
                            Level.FINE,
                            "Exception was thrown. Returning response with error code: " +
                                "'${ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS.value}'.",
                            exception
                        )
                    }
                }
            }
        }

        given("A BesluitPublicationDisabledException exception") {
            val exception = BesluitPublicationDisabledException("error")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the proper error code and no exception message and log the exception") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.besluit.publication.disabled",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
                }
            }
        }

        given("A BesluitPublicationDateMissingException exception") {
            val exception = BesluitPublicationDateMissingException()

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the proper error code and no exception message and log the exception") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.besluit.publication.date.missing",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    verify(
                        exactly = 1
                    ) {
                        log(
                            any(),
                            Level.FINE,
                            "Exception was thrown. Returning response with error code: 'msg.error.besluit.publication.date.missing'.",
                            exception
                        )
                    }
                }
            }
        }

        given("A BesluitResponseDateInvalidException exception") {
            val exception = BesluitResponseDateInvalidException("error")

            `when`("the exception is mapped to a response") {
                val response = restExceptionMapper.toResponse(exception)

                then("it should return the proper error code and no exception message and log the exception") {
                    checkResponse(
                        response = response,
                        errorMessage = "msg.error.besluit.response.date.invalid",
                        expectedStatus = HttpStatus.SC_BAD_REQUEST
                    )
                    verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
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
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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

                And("it should log the exception") {
                    verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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

                And("it should log the exception") {
                    verify(exactly = 1) { log(any(), Level.WARNING, exception.message, exception) }
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
