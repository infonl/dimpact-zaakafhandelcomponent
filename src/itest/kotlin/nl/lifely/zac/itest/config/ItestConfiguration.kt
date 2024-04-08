/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.config

import java.time.LocalDateTime
import java.util.UUID

/**
 * These values need to correspond to the test data in the databases of the various services
 * used in the entire integration test flow such as: Keycloak, Objecten, Objecttypen, Open Zaak, ZAC.
 */
object ItestConfiguration {
    const val BETROKKENE_TYPE_NATUURLIJK_PERSOON = "NATUURLIJK_PERSOON"
    const val BETROKKENE_IDENTIFICATIE_TYPE_BSN = "BSN"
    const val KEYCLOAK_HOSTNAME_URL = "http://localhost:8081"
    const val KEYCLOAK_HEALTH_READY_URL = "$KEYCLOAK_HOSTNAME_URL/health/ready"
    const val KEYCLOAK_REALM = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING = "bijlage"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID = "b1933137-94d6-49bc-9e12-afe712512276"
    const val OBJECT_PRODUCTAANVRAAG_UUID = "9dbed186-89ca-48d7-8c6c-f9995ceb8e27"
    const val OBJECTS_BASE_URI = "http://objecten-api.local:8000"
    const val OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT = "021f685e-9482-4620-b157-34cd4003da6b"
    const val OPEN_NOTIFICATIONS_API_SECRET_KEY = "openNotificatiesApiSecretKey"
    const val OPEN_ZAAK_BASE_URI = "http://openzaak.local:8000"
    const val OPEN_ZAAK_EXTERNAL_PORT = 8001
    const val OPEN_ZAAK_EXTERNAL_URI = "http://localhost:$OPEN_ZAAK_EXTERNAL_PORT"
    const val OPEN_ZAAK_CLIENT_ID = "zac_client"
    const val OPEN_ZAAK_CLIENT_SECRET = "openzaakZaakafhandelcomponentClientSecret"
    const val PRODUCT_AANVRAAG_TYPE = "productaanvraag"
    const val ROLTYPE_NAME_BETROKKENE = "Belanghebbende"
    const val ROLTYPE_UUID_BELANGHEBBENDE = "4c4cd850-8332-4bb9-adc4-dd046f0614ad"
    const val TEST_BETROKKENE_BSN_HENDRIKA_JANSE = "999993896"
    const val TEST_SPEC_ORDER_INITIAL = 0
    const val TEST_SPEC_ORDER_AFTER_ZAAK_CREATED = 1
    const val TEST_SPEC_ORDER_AFTER_TASK_CREATED = 2
    const val TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED = 3
    const val TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED = 4
    const val TEST_SPEC_ORDER_AFTER_TASK_COMPLETED = 5
    const val TEST_USER_1_ID = "testuser1"
    const val TEST_USER_1_NAME = "Test User1"
    const val TEST_USER_1_PASSWORD = "testuser1"
    const val TEST_USER_2_ID = "testuser2"
    const val TEST_USER_2_NAME = "Test User2"
    const val TEST_USER_2_PASSWORD = "testuser2"
    const val TEST_RECORD_MANAGER_1_ID = "recordmanager1"
    const val TEST_RECORD_MANAGER_1_NAME = "Test Recordmanager1"
    const val TEST_FUNCTIONAL_ADMIN_1_ID = "functioneelbeheerder1"
    const val TEST_FUNCTIONAL_ADMIN_1_NAME = "Test Functioneelbeheerder1"
    const val TEST_GROUP_A_ID = "test-group-a"
    const val TEST_GROUP_A_DESCRIPTION = "Test group A"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_ID = "test-group-fb"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION = "Test group functional admins"
    const val TEST_GROUP_RECORD_MANAGERS_ID = "test-group-rm"
    const val TEST_GROUP_RECORD_MANAGERS_DESCRIPTION = "Test group record managers"
    const val ZAC_CONTAINER_SERVICE_NAME = "zac"
    const val ZAC_CONTAINER_PORT = 8080

    /**
     * The default ZAC Docker image used when running the integration tests locally.
     * When running the tests in our GitHub pipeline a different Docker image specific for the pipeline is used.
     */
    const val ZAC_DEFAULT_DOCKER_IMAGE = "ghcr.io/infonl/zaakafhandelcomponent:dev"
    const val ZAC_MANAGEMENT_PORT = 9990

    /**
     * The ZAC API URI from outside the Docker network.
     */
    const val ZAC_API_URI = "http://localhost:$ZAC_CONTAINER_PORT/rest"

    /**
     * The ZAC websocket base URI from outside the Docker network.
     */
    const val ZAC_WEBSOCKET_BASE_URI = "ws://localhost:$ZAC_CONTAINER_PORT/websocket"

    /**
     * The ZAC management URI from outside the Docker network.
     */
    const val ZAC_MANAGEMENT_URI = "http://localhost:$ZAC_MANAGEMENT_PORT"
    const val ZAC_HEALTH_READY_URL = "$ZAC_MANAGEMENT_URI/health/ready"
    const val ZAAK_1_IDENTIFICATION = "ZAAK-2023-0000000001"
    const val ZAAK_2_IDENTIFICATION = "ZAAK-2023-0000000002"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE = "melding-evenement-organiseren-behandelen"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION = "Melding evenement organiseren behandelen"
    const val SMARTDOCUMENTS_MOCK_BASE_URI = "http://smartdocuments-wiremock:8080"

    val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID: UUID = UUID.fromString("448356ff-dcfb-4504-9501-7fe929077c4f")
    val START_DATE = LocalDateTime.now()
}
