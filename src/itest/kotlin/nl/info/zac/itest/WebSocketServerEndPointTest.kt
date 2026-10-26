/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAtLeastOne
import io.kotest.inspectors.forNone
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_BEHANDELAAR
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAAK_ROLLEN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_WEBSOCKET_BASE_URI
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.util.WebSocketTestListener
import okhttp3.Headers
import okhttp3.WebSocket
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private const val SCREEN_EVENT_TYPE_ZAAK = "ZAAK"
private const val SCREEN_EVENT_TYPE_TAAK = "TAAK"
private const val SCREEN_EVENT_TYPE_ZAAK_TAKEN = "ZAAK_TAKEN"
private const val SCREEN_EVENT_TYPE_ZAAK_NOTITIES = "ZAAK_NOTITIES"
private const val SCREEN_EVENT_TYPE_SIGNALERINGEN = "SIGNALERINGEN"
private const val SCREEN_EVENT_TYPE_ANY = "ANY"

class WebSocketServerEndPointTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val taskHelper = TaskHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)
    val testStartEpochSecond = Instant.now().epochSecond
    lateinit var zaakUuid: UUID

    fun subscriptionMessage(subscriptionType: String, objectType: String, resource: Any) = """
        {
            "subscriptionType": "$subscriptionType",
            "event": {
                "opcode": "ANY",
                "objectType": "$objectType",
                "objectId": { "resource": "$resource" },
                "_key": "ANY;$objectType;$resource"
            }
        }
    """.trimIndent()

    fun connectWebSocket(webSocketTestListener: WebSocketTestListener): WebSocket =
        itestHttpClient.connectNewWebSocket(
            url = ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = webSocketTestListener,
            testUser = RAADPLEGER_1
        )

    fun sendNotification(
        resource: String,
        resourceUrl: String,
        kanaal: String = "zaken",
        hoofdObject: String = "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
        actie: String = "partial_update"
    ) {
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "kanaal" to kanaal,
                    "resource" to resource,
                    "resourceUrl" to resourceUrl,
                    "hoofdObject" to hoofdObject,
                    "actie" to actie,
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                    "kenmerken" to mapOf(
                        "bronorganisatie" to "123443210",
                        "vertrouwelijkheidaanduiding" to "openbaar"
                    ) + if (kanaal == "zaken") {
                        mapOf("zaaktype" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_CMMN_TEST_3_UUID")
                    } else {
                        emptyMap()
                    }
                )
            ).toString()
        ).code shouldBe HTTP_NO_CONTENT
    }

    fun sendZaakNotification() = sendNotification("zaak", "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid")

    fun sendRolNotification() =
        sendNotification("rol", "$OPEN_ZAAK_BASE_URI/zaken/api/v1/rollen/${UUID.randomUUID()}")

    fun List<String>.containsEventFor(objectType: String) =
        any { JSONObject(it).getString("objectType") == objectType }

    suspend fun awaitZaakEvent(webSocketTestListener: WebSocketTestListener) =
        eventually(30.seconds) {
            sendZaakNotification()
            webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK) shouldBe true
        }

    suspend fun awaitRolEvent(webSocketTestListener: WebSocketTestListener) =
        eventually(30.seconds) {
            sendRolNotification()
            webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK_ROLLEN) shouldBe true
        }

    beforeSpec {
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_3_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            code shouldBe HTTP_OK
            zaakUuid = JSONObject(bodyAsString).getString("uuid").let(UUID::fromString)
        }
    }

    context("Receiving a screen event on a websocket") {
        given("a websocket that subscribes to all changes of a zaak") {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            connectWebSocket(webSocketTestListener)

            `when`("a notification is sent to ZAC that the zaak has been updated") {
                eventually(30.seconds) {
                    sendZaakNotification()
                    webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK) shouldBe true
                }

                then("the websocket receives the complete screen event, with the id of the zaak and without a detail") {
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            keySet() shouldBe setOf("timestamp", "opcode", "objectType", "objectId", "actorUsername")
                            getLong("timestamp") shouldBeGreaterThanOrEqual testStartEpochSecond
                            getString("opcode") shouldBe "UPDATED"
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK
                            getJSONObject("objectId").getString("resource") shouldBe zaakUuid.toString()
                            getJSONObject("objectId").isNull("detail") shouldBe true
                            getString("actorUsername").shouldNotBeBlank()
                        }
                    }
                }
            }
        }
    }

    context("Receiving the screen events of a taak on a websocket") {
        given(
            "a zaak with an open taak, and a websocket that subscribes to all changes of the taak " +
                "and to all changes of the taken of the zaak"
        ) {
            val zaakMetTaakJson = zacClient.createZaakAndRetrieve(
                zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
                groupId = GROUP_BEHANDELAARS_TEST_1.name,
                groupName = GROUP_BEHANDELAARS_TEST_1.description,
                startDate = DATE_TIME_2000_01_01,
                testUser = BEHANDELAAR_1
            ).run {
                code shouldBe HTTP_OK
                JSONObject(bodyAsString)
            }
            val zaakMetTaakUuid = zaakMetTaakJson.getString("uuid").let(UUID::fromString)
            val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
                zaakUuid = zaakMetTaakUuid,
                zaakIdentificatie = zaakMetTaakJson.getString("identificatie"),
                fatalDate = LocalDate.now().plusWeeks(1),
                group = GROUP_BEHANDELAARS_TEST_1,
                testUser = BEHANDELAAR_1
            )
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_TAAK, taskId)
            )
            connectWebSocket(webSocketTestListener)
                .send(subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK_TAKEN, zaakMetTaakUuid))

            `when`("a behandelaar assigns the taak to themselves") {
                eventually(30.seconds) {
                    itestHttpClient.performPatchRequest(
                        url = "$ZAC_API_URI/taken/toekennen",
                        requestBodyAsString = """
                            {
                                "taakId": "$taskId",
                                "zaakUuid": "$zaakMetTaakUuid",
                                "groepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                                "behandelaarId": "${BEHANDELAAR_1.username}",
                                "reden": "fakeReason"
                            }
                        """.trimIndent(),
                        testUser = BEHANDELAAR_1
                    ).code shouldBe HTTP_NO_CONTENT
                    with(webSocketTestListener.messagesReceived) {
                        containsEventFor(SCREEN_EVENT_TYPE_TAAK) shouldBe true
                        containsEventFor(SCREEN_EVENT_TYPE_ZAAK_TAKEN) shouldBe true
                    }
                }

                then("the websocket receives an update of the taak, caused by the behandelaar") {
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            getString("opcode") shouldBe "UPDATED"
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_TAAK
                            getJSONObject("objectId").getString("resource") shouldBe taskId
                            getJSONObject("objectId").isNull("detail") shouldBe true
                            getString("actorUsername") shouldBe BEHANDELAAR_1.username
                        }
                    }
                }

                and("the websocket receives an update of the taken of the zaak, caused by the behandelaar") {
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            getString("opcode") shouldBe "UPDATED"
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK_TAKEN
                            getJSONObject("objectId").getString("resource") shouldBe zaakMetTaakUuid.toString()
                            getJSONObject("objectId").isNull("detail") shouldBe true
                            getString("actorUsername") shouldBe BEHANDELAAR_1.username
                        }
                    }
                }
            }
        }
    }

    context("Receiving the screen events of notifications about zaken, besluiten and documenten on a websocket") {
        data class NotifiedResource(
            val kanaal: String,
            val resource: String,
            val hoofdObjectPath: String,
            val resourcePath: String?,
            val expectedScreenEventType: String
        )

        listOf(
            NotifiedResource(
                kanaal = "besluiten",
                resource = "besluit",
                hoofdObjectPath = "besluiten/api/v1/besluiten",
                resourcePath = null,
                expectedScreenEventType = "BESLUIT"
            ),
            NotifiedResource(
                kanaal = "besluiten",
                resource = "besluitinformatieobject",
                hoofdObjectPath = "besluiten/api/v1/besluiten",
                resourcePath = "besluiten/api/v1/besluitinformatieobjecten",
                expectedScreenEventType = "BESLUIT_INFORMATIEOBJECTEN"
            ),
            NotifiedResource(
                kanaal = "documenten",
                resource = "enkelvoudiginformatieobject",
                hoofdObjectPath = "documenten/api/v1/enkelvoudiginformatieobjecten",
                resourcePath = null,
                expectedScreenEventType = "ENKELVOUDIG_INFORMATIEOBJECT"
            ),
            NotifiedResource(
                kanaal = "documenten",
                resource = "gebruiksrechten",
                hoofdObjectPath = "documenten/api/v1/enkelvoudiginformatieobjecten",
                resourcePath = "documenten/api/v1/gebruiksrechten",
                expectedScreenEventType = "ENKELVOUDIG_INFORMATIEOBJECT"
            ),
            NotifiedResource(
                kanaal = "zaken",
                resource = "zaakinformatieobject",
                hoofdObjectPath = "zaken/api/v1/zaken",
                resourcePath = "zaken/api/v1/zaakinformatieobjecten",
                expectedScreenEventType = "ZAAK_INFORMATIEOBJECTEN"
            ),
            NotifiedResource(
                kanaal = "zaken",
                resource = "zaakbesluit",
                hoofdObjectPath = "zaken/api/v1/zaken",
                resourcePath = "zaken/api/v1/zaakbesluiten",
                expectedScreenEventType = "ZAAK_BESLUITEN"
            )
        ).forEach { notifiedResource ->
            given(
                "a websocket that subscribes to all ${notifiedResource.expectedScreenEventType} changes of an object, " +
                    "and a notification about a ${notifiedResource.resource} of that object"
            ) {
                val hoofdObjectUuid = UUID.randomUUID()
                val resourceUuid = notifiedResource.resourcePath?.let { UUID.randomUUID() }
                val hoofdObjectUrl = "$OPEN_ZAAK_BASE_URI/${notifiedResource.hoofdObjectPath}/$hoofdObjectUuid"
                val webSocketTestListener = WebSocketTestListener(
                    textToBeSentOnOpen = subscriptionMessage(
                        "CREATE",
                        notifiedResource.expectedScreenEventType,
                        hoofdObjectUuid
                    )
                )
                connectWebSocket(webSocketTestListener)

                `when`("the notification is sent to ZAC") {
                    eventually(30.seconds) {
                        sendNotification(
                            resource = notifiedResource.resource,
                            resourceUrl = notifiedResource.resourcePath
                                ?.let { "$OPEN_ZAAK_BASE_URI/$it/$resourceUuid" }
                                ?: hoofdObjectUrl,
                            kanaal = notifiedResource.kanaal,
                            hoofdObject = hoofdObjectUrl
                        )
                        webSocketTestListener.messagesReceived
                            .containsEventFor(notifiedResource.expectedScreenEventType) shouldBe true
                    }

                    then(
                        "the websocket receives an update of the object, " +
                            "with the id of the ${notifiedResource.resource} as detail if it is not the object itself"
                    ) {
                        webSocketTestListener.messagesReceived.forAtLeastOne {
                            with(JSONObject(it)) {
                                getString("opcode") shouldBe "UPDATED"
                                getString("objectType") shouldBe notifiedResource.expectedScreenEventType
                                with(getJSONObject("objectId")) {
                                    getString("resource") shouldBe hoofdObjectUuid.toString()
                                    if (resourceUuid == null) {
                                        isNull("detail") shouldBe true
                                    } else {
                                        getString("detail") shouldBe resourceUuid.toString()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    context("Receiving the screen events of zaak notities on a websocket") {
        given("a websocket that subscribes to all changes of the notities of a zaak") {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK_NOTITIES, zaakUuid)
            )
            connectWebSocket(webSocketTestListener)

            `when`("a behandelaar adds a notitie to the zaak") {
                eventually(30.seconds) {
                    itestHttpClient.performJSONPostRequest(
                        url = "$ZAC_API_URI/notities",
                        requestBodyAsString = """
                            {
                                "zaakUUID": "$zaakUuid",
                                "tekst": "fakeNoteText",
                                "gebruikersnaamMedewerker": "${BEHANDELAAR_1.username}"
                            }
                        """.trimIndent(),
                        testUser = BEHANDELAAR_1
                    ).code shouldBe HTTP_OK
                    webSocketTestListener.messagesReceived
                        .containsEventFor(SCREEN_EVENT_TYPE_ZAAK_NOTITIES) shouldBe true
                }

                then("the websocket receives an update of the notities of the zaak, caused by the behandelaar") {
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            getString("opcode") shouldBe "UPDATED"
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK_NOTITIES
                            getJSONObject("objectId").getString("resource") shouldBe zaakUuid.toString()
                            getString("actorUsername") shouldBe BEHANDELAAR_1.username
                        }
                    }
                }
            }
        }
    }

    context("Receiving the screen events of signaleringen on a websocket") {
        given(
            "a behandelaar who shows 'zaak op naam' signaleringen on the dashboard, a zaak assigned to this behandelaar, " +
                "and a websocket that subscribes to all changes of the signaleringen of the behandelaar"
        ) {
            itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/signaleringen/instellingen",
                headers = Headers.headersOf("Content-Type", "application/json"),
                requestBodyAsString = """
                    {"isDashboardEnabled":true,"isMailEnabled":false,"subjecttype":"ZAAK","type":"ZAAK_OP_NAAM"}
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).code shouldBe HTTP_OK
            val zaakOpNaamUuid = zacClient.createZaakAndRetrieve(
                zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
                groupId = GROUP_BEHANDELAARS_TEST_1.name,
                groupName = GROUP_BEHANDELAARS_TEST_1.description,
                behandelaarId = BEHANDELAAR_1.username,
                startDate = DATE_TIME_2000_01_01,
                testUser = BEHANDELAAR_1
            ).run {
                code shouldBe HTTP_OK
                JSONObject(bodyAsString).getString("uuid").let(UUID::fromString)
            }
            val behandelaarRolUrl = JSONObject(openZaakClient.getRolesForZaak(zaakOpNaamUuid).bodyAsString)
                .getJSONArray("results")
                .let { rollen -> (0 until rollen.length()).map(rollen::getJSONObject) }
                .single {
                    it.getString("omschrijving") == ROLTYPE_NAME_BEHANDELAAR &&
                        it.getString("betrokkeneType") == "medewerker"
                }
                .getString("url")
                .replace(OPEN_ZAAK_EXTERNAL_URI, OPEN_ZAAK_BASE_URI)
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_SIGNALERINGEN, BEHANDELAAR_1.username)
            )
            connectWebSocket(webSocketTestListener)

            `when`("a notification is sent to ZAC that the behandelaar rol of the zaak has been created") {
                eventually(30.seconds) {
                    sendNotification(
                        resource = "rol",
                        resourceUrl = behandelaarRolUrl,
                        hoofdObject = "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakOpNaamUuid",
                        actie = "create"
                    )
                    webSocketTestListener.messagesReceived
                        .containsEventFor(SCREEN_EVENT_TYPE_SIGNALERINGEN) shouldBe true
                }

                then("the websocket receives an update of the 'zaak op naam' signaleringen of the behandelaar") {
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            getString("opcode") shouldBe "UPDATED"
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_SIGNALERINGEN
                            getJSONObject("objectId").getString("resource") shouldBe BEHANDELAAR_1.username
                            getJSONObject("objectId").getString("detail") shouldBe "ZAAK_OP_NAAM"
                        }
                    }
                }
            }
        }
    }

    context("Subscribing to all screen event types of an object") {
        given("a websocket that subscribes to all changes of all types for a zaak") {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ANY, zaakUuid)
            )
            connectWebSocket(webSocketTestListener)

            `when`("notifications are sent to ZAC that the zaak and one of its rollen have been updated") {
                eventually(30.seconds) {
                    sendZaakNotification()
                    sendRolNotification()
                    with(webSocketTestListener.messagesReceived) {
                        containsEventFor(SCREEN_EVENT_TYPE_ZAAK) shouldBe true
                        containsEventFor(SCREEN_EVENT_TYPE_ZAAK_ROLLEN) shouldBe true
                    }
                }

                then("the websocket receives the updates of both the zaak and its rollen") {
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK
                            getJSONObject("objectId").getString("resource") shouldBe zaakUuid.toString()
                        }
                    }
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK_ROLLEN
                            getJSONObject("objectId").getString("resource") shouldBe zaakUuid.toString()
                        }
                    }
                }
            }
        }
    }

    context("Cancelling a websocket subscription") {
        given("a websocket that subscribes to all changes of a zaak and then cancels that subscription") {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            val webSocket = connectWebSocket(webSocketTestListener)
            awaitZaakEvent(webSocketTestListener)
            webSocket.send(subscriptionMessage("DELETE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid))
            webSocket.send(subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK_ROLLEN, zaakUuid))
            awaitRolEvent(webSocketTestListener)
            webSocketTestListener.messagesReceived.clear()

            `when`("notifications are sent to ZAC that the zaak and one of its rollen have been updated") {
                eventually(30.seconds) {
                    sendZaakNotification()
                    sendRolNotification()
                    webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK_ROLLEN) shouldBe true
                }

                then("the websocket only receives events for the rollen and never for the zaak") {
                    continually(5.seconds) {
                        webSocketTestListener.messagesReceived.forNone {
                            JSONObject(it).getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK
                        }
                    }
                }
            }
        }

        given(
            "two websockets that both subscribe to all changes of the same zaak, " +
                "after which the first websocket cancels all its subscriptions"
        ) {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            val otherWebSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            val webSocket = connectWebSocket(webSocketTestListener)
            connectWebSocket(otherWebSocketTestListener)
            awaitZaakEvent(webSocketTestListener)
            awaitZaakEvent(otherWebSocketTestListener)
            webSocket.send("""{ "subscriptionType": "DELETE_ALL" }""")
            webSocket.send(subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK_ROLLEN, zaakUuid))
            awaitRolEvent(webSocketTestListener)
            webSocketTestListener.messagesReceived.clear()
            otherWebSocketTestListener.messagesReceived.clear()

            `when`("a notification is sent to ZAC that the zaak has been updated") {
                eventually(30.seconds) {
                    sendZaakNotification()
                    otherWebSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK) shouldBe true
                }

                then("the second websocket keeps its subscription and only it receives the event for the zaak") {
                    continually(5.seconds) {
                        webSocketTestListener.messagesReceived.forNone {
                            JSONObject(it).getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK
                        }
                    }
                }
            }
        }
    }
})
