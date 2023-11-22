/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.config

import org.mockserver.client.MockServerClient
import org.testcontainers.utility.DockerImageName

/**
 * These values need to correspond to the test data in the databases of the various services
 * used in the entire integration test flow such as: Keycloak, Objecten, Objecttypen, Open Zaak, ZAC.
 */
object ItestConfiguration {
    const val KEYCLOAK_HOSTNAME_URL = "http://localhost:8081"
    const val KEYCLOAK_REALM = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"
    const val OBJECT_PRODUCTAANVRAAG_UUID = "9dbed186-89ca-48d7-8c6c-f9995ceb8e27"
    const val OBJECTS_API_HOSTNAME_URL = "http://objecten-api.local:8000"
    const val OBJECTTYPE_UUID_PRODUCTAANVRAAG_DENHAAG = "021f685e-9482-4620-b157-34cd4003da6b"
    const val OPEN_NOTIFICATIONS_API_SECRET_KEY = "openNotificatiesApiSecretKey"
    const val PRODUCT_AANVRAAG_TYPE = "productaanvraag"
    const val ZAC_CONTAINER_PORT = 8080
    const val ZAC_MANAGEMENT_PORT = 9990

    // the ZAC API URI from outside the Docker network
    const val ZAC_API_URI = "http://localhost:$ZAC_CONTAINER_PORT/rest"

    // the ZAC management URI from outside the Docker network
    const val ZAC_MANAGEMENT_URI = "http://localhost:$ZAC_MANAGEMENT_PORT"
    const val ZAAK_1_IDENTIFICATION = "ZAAK-2023-0000000001"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID = "448356ff-dcfb-4504-9501-7fe929077c4f"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE = "melding-evenement-organiseren-behandelen"

    val MOCKSERVER_IMAGE = DockerImageName
        .parse("mockserver/mockserver")
        .withTag(
            "mockserver-" + MockServerClient::class.java.getPackage().implementationVersion
        )
}
