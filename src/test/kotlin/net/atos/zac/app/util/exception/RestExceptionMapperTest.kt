/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.util.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.client.bag.BagClientService
import net.atos.client.klant.KlantClientService
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.exception.BrcRuntimeException
import net.atos.client.zgw.drc.exception.DrcRuntimeException
import net.atos.client.zgw.shared.exception.ZgwRuntimeException
import net.atos.client.zgw.zrc.exception.ZrcRuntimeException
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.exception.ZtcRuntimeException
import net.atos.zac.app.decision.DecisionPublicationDateMissingException
import net.atos.zac.app.decision.DecisionPublicationDisabledException
import net.atos.zac.app.decision.DecisionResponseDateInvalidException
import net.atos.zac.app.decision.DecisionResponseDateMissingException
import net.atos.zac.app.exception.RestExceptionMapper
import net.atos.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import net.atos.zac.smartdocuments.exception.SmartDocumentsDisabledException
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
        val exceptionMessage = "DummyRuntimeException"
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
        val exceptionMessage = "DummyRuntimeException"
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
        val exceptionMessage = "DummyRuntimeException"
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
        val exceptionMessage = "DummyRuntimeException"
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
        val exceptionMessage = "DummyRuntimeException"
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
        val exceptionMessage = "DummyRuntimeException"
        val exception = ZtcRuntimeException(exceptionMessage)

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the ZTC server error code and the exception message and log the exception") {
                checkResponse(response, "msg.error.ztc.client.exception")
                verify(exactly = 1) { log(any(), Level.SEVERE, exception.message!!, exception) }
            }
        }
    }
    Given(
        """
        A JAX-RS processing exception with as root cause a HttpHostConnectException
        and which contains the BAG client service class name in the stacktrace
        """
    ) {
        val exceptionMessage = "DummyProcessingException"
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
        val exceptionMessage = "DummyProcessingException"
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
        val exceptionMessage = "DummyProcessingException"
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
        val exceptionMessage = "DummyProcessingException"
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
        val exceptionMessage = "DummyProcessingException"
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
        val exceptionMessage = "DummyProcessingException"
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
    Given("A SmartDocumentsDisabled exception") {
        val exception = SmartDocumentsDisabledException("disabled")

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the proper error code and no exception message and log the exception") {
                checkResponse(
                    response = response,
                    errorMessage = "msg.error.smartdocuments.disabled",
                    expectedStatus = HttpStatus.SC_BAD_REQUEST
                )
                verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
            }
        }
    }
    Given("A SmartDocumentsConfiguration exception") {
        val exception = SmartDocumentsConfigurationException("error")

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the proper error code and no exception message and log the exception") {
                checkResponse(
                    response = response,
                    errorMessage = "msg.error.smartdocuments.not.configured",
                    expectedStatus = HttpStatus.SC_BAD_REQUEST
                )
                verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
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
        val exception = DecisionPublicationDateMissingException("error")

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the proper error code and no exception message and log the exception") {
                checkResponse(
                    response = response,
                    errorMessage = "msg.error.besluit.publication.date.missing",
                    expectedStatus = HttpStatus.SC_BAD_REQUEST
                )
                verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
            }
        }
    }
    Given("A DecisionResponseDateMissingException exception") {
        val exception = DecisionResponseDateMissingException("error")

        When("the exception is mapped to a response") {
            val response = restExceptionMapper.toResponse(exception)

            Then("it should return the proper error code and no exception message and log the exception") {
                checkResponse(
                    response = response,
                    errorMessage = "msg.error.besluit.response.date.missing",
                    expectedStatus = HttpStatus.SC_BAD_REQUEST
                )
                verify(exactly = 1) { log(any(), Level.FINE, exception.message!!, exception) }
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
