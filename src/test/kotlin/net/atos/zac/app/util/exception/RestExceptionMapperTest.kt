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
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.client.bag.BagClientService
import net.atos.client.klant.KlantClientService
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.exception.DrcRuntimeException
import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.client.zgw.shared.model.createFieldValidationError
import net.atos.client.zgw.shared.model.createValidationZgwError
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.exception.BrcRuntimeException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.exception.ZtcRuntimeException
import nl.info.zac.app.decision.DecisionPublicationDateMissingException
import nl.info.zac.app.decision.DecisionPublicationDisabledException
import nl.info.zac.app.decision.DecisionResponseDateInvalidException
import nl.info.zac.app.exception.RestExceptionMapper
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BAG_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.exception.ServerErrorException
import nl.info.zac.log.log
import org.apache.http.HttpHost
import org.apache.http.HttpStatus
import org.apache.http.conn.HttpHostConnectException
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException
import java.util.logging.Level

class RestExceptionMapperTest : BehaviorSpec({
    val restExceptionMapper = RestExceptionMapper()

    beforeSpec {
        mockkStatic(::log)
    }

    afterSpec {
        unmockkStatic(::log)
    }

    Given("A runtime exception") {
        val exceptionMessage = "FakeRuntimeException"
        val exception = RuntimeException(exceptionMessage)

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the generic server error code and the exception message and log the exception") {
                checkResponse(response, "msg.error.server.generic", exceptionMessage)
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given("A BRC runtime exception") {
        val exceptionMessage = "FakeRuntimeException"
        val exception = BrcRuntimeException(exceptionMessage)

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the ZTC server error code and log the exception") {
                checkResponse(response, "msg.error.brc.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given("A DRC runtime exception") {
        val exceptionMessage = "FakeRuntimeException"
        val exception = DrcRuntimeException(exceptionMessage)

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the DRC server error code and log the exception") {
                checkResponse(response, "msg.error.drc.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given("A ZRC runtime exception") {
        val exceptionMessage = "FakeRuntimeException"
        val exception = ZrcRuntimeException(exceptionMessage)

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the ZRC server error code and log the exception") {
                checkResponse(response, "msg.error.zrc.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given("A ZGW runtime exception") {
        val exceptionMessage = "FakeRuntimeException"
        val exception = ZgwRuntimeException(exceptionMessage)

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the generic server error code and the exception message and log the exception") {
                checkResponse(response, "msg.error.server.generic", exceptionMessage)
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given("A ZTC runtime exception") {
        val exceptionMessage = "FakeRuntimeException"
        val exception = ZtcRuntimeException(exceptionMessage)

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the ZTC server error code and the exception message and log the exception") {
                checkResponse(response, "msg.error.ztc.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given("A ZGW validation error exception with two invalid parameters") {
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(zgwValidationErrorException)

            Then(
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

    Given(
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(flowableZgwValidationErrorException)

            Then(
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

    Given(
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the BAG server error code and no exception message and log the exception") {
                checkResponse(response, "msg.error.bag.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given(
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the BRC server error code and no exception message and log the exception") {
                checkResponse(response, "msg.error.brc.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given(
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the Klanten server error code and no exception message and log the exception") {
                checkResponse(response, "msg.error.klanten.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given(
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the Objecten server error code and no exception message and log the exception") {
                checkResponse(response, "msg.error.objects.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given(
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the ZTC server error code and no exception message and log the exception") {
                checkResponse(response, "msg.error.ztc.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given(
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

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then(
                "it should return the general server error error code with an exception message and log the exception"
            ) {
                checkResponse(response, "msg.error.server.generic", exceptionMessage)
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }

    Given("An input validation exception with an error code and a message") {
        val exception = InputValidationFailedException(
            errorCode = ERROR_CODE_BAG_CLIENT,
            message = "fakeErrorMessage"
        )

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then(
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

    Given("An input validation exception with an error code and no message") {
        val exception = InputValidationFailedException(
            errorCode = ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
        )

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then(
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

    Given("A DecisionPublicationDisabledException exception") {
        val exception = DecisionPublicationDisabledException("error")

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the proper error code and no exception message and log the exception") {
                checkResponse(
                    response = response,
                    errorMessage = "msg.error.besluit.publication.disabled",
                    expectedStatus = HttpStatus.SC_BAD_REQUEST
                )
                verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
            }
        }
    }

    Given("A DecisionPublicationDateMissingException exception") {
        val exception = DecisionPublicationDateMissingException()

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the proper error code and no exception message and log the exception") {
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

    Given("A DecisionResponseDateInvalidException exception") {
        val exception = DecisionResponseDateInvalidException("error")

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the proper error code and no exception message and log the exception") {
                checkResponse(
                    response = response,
                    errorMessage = "msg.error.besluit.response.date.invalid",
                    expectedStatus = HttpStatus.SC_BAD_REQUEST
                )
                verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
            }
        }
    }
    Given("A server error exception") {
        val errorCode = mockk<ErrorCode>()
        val exception = ServerErrorException(errorCode, "error")
        every { errorCode.value } returns "fakeErrorCodeValue"

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the expected error code and no exception message and log the exception") {
                checkResponse(
                    response = response,
                    errorMessage = "fakeErrorCodeValue",
                    expectedStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
                )
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
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
