/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest.config

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.UUID

/**
 * These values need to correspond to the test data in the databases of the various services
 * used in the entire integration test flow such as: Keycloak, Objecten, Objecttypen, Open Zaak, ZAC.
 */
object ItestConfiguration {
    private const val ZAC_CONTAINER_PORT = 8080
    private const val ZAC_MANAGEMENT_PORT = 9990

    const val HTTP_STATUS_OK = 200
    const val HTTP_STATUS_NO_CONTENT = 204
    const val HTTP_STATUS_BAD_REQUEST = 400
    const val HTTP_STATUS_FORBIDDEN = 403

    const val ACTIE_INTAKE_AFRONDEN = "INTAKE_AFRONDEN"
    const val ACTIE_ZAAK_AFHANDELEN = "ZAAK_AFHANDELEN"
    const val BETROKKENE_TYPE_NATUURLIJK_PERSOON = "NATUURLIJK_PERSOON"
    const val BETROKKENE_IDENTIFICATION_TYPE_BSN = "BSN"
    const val BETROKKENE_IDENTIFACTION_TYPE_VESTIGING = "VN"
    const val COMMUNICATIEKANAAL_EMAIL = "f5de7d7f-8440-4ce7-8f27-f934ad0c2ea6"
    const val FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE = "AANVULLENDE_INFORMATIE"
    const val HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM = "Aanvullende informatie"
    const val KEYCLOAK_HOSTNAME_URL = "http://localhost:8081"
    const val KEYCLOAK_HEALTH_READY_URL = "$KEYCLOAK_HOSTNAME_URL/health/ready"
    const val KEYCLOAK_REALM = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING = "bijlage"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID = "b1933137-94d6-49bc-9e12-afe712512276"
    const val OBJECT_PRODUCTAANVRAAG_1_UUID = "9dbed186-89ca-48d7-8c6c-f9995ceb8e27"
    const val OBJECT_PRODUCTAANVRAAG_2_UUID = "f1f6f670-fda8-4e98-81a6-6528937f10ee"
    const val OBJECTS_BASE_URI = "http://objecten-api.local:8000"
    const val OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT = "021f685e-9482-4620-b157-34cd4003da6b"
    const val OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_1_BRON_KENMERK = "f8534f13-0669-4d4d-a364-6b6c4ad3d243"
    const val OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK = "dca40822-5eb3-4acc-b915-7b020041ad55"
    const val OPEN_FORMULIEREN_FORMULIER_BRON_NAAM = "open-forms"
    const val OPEN_NOTIFICATIONS_API_SECRET_KEY = "openNotificatiesApiSecretKey"
    const val OPEN_ZAAK_BASE_URI = "http://openzaak.local:8000"
    const val OPEN_ZAAK_EXTERNAL_PORT = 8001
    const val OPEN_ZAAK_EXTERNAL_URI = "http://localhost:$OPEN_ZAAK_EXTERNAL_PORT"
    const val OPEN_ZAAK_CLIENT_ID = "zac_client"
    const val OPEN_ZAAK_CLIENT_SECRET = "openzaakZaakafhandelcomponentClientSecret"
    const val PDF_MIME_TYPE = "application/pdf"
    const val PRODUCTAANVRAAG_TYPE_1 = "productaanvraag-type-1"
    const val PRODUCTAANVRAAG_TYPE_2 = "productaanvraag-type-2"
    const val PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LATITUDE = 52.08968250760225
    const val PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LONGITUDE = 5.114358701512936
    const val ROLTYPE_NAME_BETROKKENE = "Belanghebbende"
    const val ROLTYPE_UUID_BELANGHEBBENDE = "4c4cd850-8332-4bb9-adc4-dd046f0614ad"
    const val ROLTYPE_COUNT = 16
    const val SCREEN_EVENT_TYPE_TAKEN_VERDELEN = "TAKEN_VERDELEN"
    const val SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN = "TAKEN_VRIJGEVEN"
    const val SCREEN_EVENT_TYPE_ZAKEN_SIGNALERINGEN = "ZAKEN_SIGNALERINGEN"
    const val SCREEN_EVENT_TYPE_ZAKEN_VERDELEN = "ZAKEN_VERDELEN"
    const val SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN = "ZAKEN_VRIJGEVEN"

    /**
     * Test person that exists in both the BRP and the Klanten API databases
     */
    const val TEST_PERSON_HENDRIKA_JANSE_BSN = "999993896"
    const val TEST_PERSON_HENDRIKA_JANSE_EMAIL = "hendrika.janse@example.com"
    const val TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE = "1965-01-01"
    const val TEST_PERSON_HENDRIKA_JANSE_GENDER = "V"
    const val TEST_PERSON_HENDRIKA_JANSE_FULLNAME = "Héndrika Janse"
    const val TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER = "0612345678"
    const val TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE =
        "Street ¦ 38 & House ¦ 10, Baghdad, Park Al-Sadoum, Hay Al-Nidhal 103"
    const val TEST_PDF_FILE_NAME = "dummyTestDocument.pdf"
    const val TEST_PDF_FILE_SIZE = 9268
    const val TEST_TXT_FILE_NAME = "testTextDocument.txt"
    const val TEST_TXT_FILE_SIZE = 63
    const val TEST_SPEC_ORDER_INITIAL = 0
    const val TEST_SPEC_ORDER_AFTER_ZAAK_CREATED = 1
    const val TEST_SPEC_ORDER_AFTER_TASK_CREATED = 2
    const val TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED = 3
    const val TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED = 4
    const val TEST_SPEC_ORDER_AFTER_TASK_COMPLETED = 5
    const val TEST_USER_1_USERNAME = "testuser1"
    const val TEST_USER_1_PASSWORD = "testuser1"
    const val TEST_USER_1_NAME = "Test User1 Špëçîâl Characters"
    const val TEST_USER_2_ID = "testuser2"
    const val TEST_USER_2_NAME = "Test User2"
    const val TEST_RECORD_MANAGER_1_USERNAME = "recordmanager1"
    const val TEST_RECORD_MANAGER_1_NAME = "Test Recordmanager1"
    const val TEST_FUNCTIONAL_ADMIN_1_ID = "functioneelbeheerder1"
    const val TEST_FUNCTIONAL_ADMIN_1_NAME = "Test Functioneelbeheerder1"
    const val TEST_GROUP_A_ID = "test-group-a"
    const val TEST_GROUP_A_DESCRIPTION = "Test group A"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_ID = "test-group-fb"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION = "Test group functional admins"
    const val TEST_GROUP_RECORD_MANAGERS_ID = "test-group-rm"
    const val TEST_GROUP_RECORD_MANAGERS_DESCRIPTION = "Test group record managers"
    const val TEST_KVK_VESTIGINGSNUMMER_1 = "000038509520"
    const val TEXT_MIME_TYPE = "application/text"

    const val SMART_DOCUMENTS_MOCK_BASE_URI = "http://smartdocuments-wiremock:8080"

    /**
     * Constants used in the SmartDocuments WireMock template response
     */
    const val SMART_DOCUMENTS_ROOT_GROUP_ID = "D5037631FF6748269059B353699EFA0C"
    const val SMART_DOCUMENTS_ROOT_GROUP_NAME = "root"
    const val SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID = "445E1A2C5D964A33961CA46679AB51CF"
    const val SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME = "root template 1"
    const val SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID = "8CCCF38A7757473CB5F5F2B8E5D7484D"
    const val SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME = "root template 2"
    const val SMART_DOCUMENTS_GROUP_1_ID = "0E18B04EDF9646C0A2D04E651DC4C6FF"
    const val SMART_DOCUMENTS_GROUP_1_NAME = "group 1"
    const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID = "7B7857BB9959470C82974037304E433D"
    const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME = "group 1 template 1"
    const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID = "273C2707E5A844699B653C87ACFD618E"
    const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME = "group 1 template 2"
    const val SMART_DOCUMENTS_GROUP_2_ID = "348097107FA346DC9AEBBE33A5500B79"
    const val SMART_DOCUMENTS_GROUP_2_NAME = "group 2"
    const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID = "7B7857BB9959470C82974037304E433D"
    const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME = "group 2 template 1"
    const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID = "273C2707E5A844699B653C87ACFD618E"
    const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME = "group 2 template 2"

    const val SMTP_SERVER_PORT = 25

    /**
     * First zaak created from a productaanvraag.
     * Currently, the zaak identification is not set by ZAC but generated by OpenZaak using the format:
     * 'ZAAK-YYYY-SEQUENCE_NUMBER' where the year is taken from the start date of the zaak.
     */
    const val ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION = "ZAAK-1970-0000000001"
    const val ZAAK_PRODUCTAANVRAAG_1_START_DATE = "1970-01-01"
    const val ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING = "1970-01-15"

    /**
     * First zaak created from a productaanvraag.
     * Currently, the zaak identification is not set by ZAC but generated by OpenZaak using the format:
     * 'ZAAK-YYYY-SEQUENCE_NUMBER' where the year is taken from the start date of the zaak.
     */
    const val ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION = "ZAAK-1999-0000000001"

    /**
     * First 'manually' created zaak using the ZAC API.
     */
    const val ZAAK_MANUAL_1_IDENTIFICATION = "ZAAK-2020-0000000001"

    const val ZAC_CONTAINER_SERVICE_NAME = "zac"

    /**
     * The default ZAC Docker image used when running the integration tests locally.
     * When running the tests in our GitHub pipeline a different Docker image specific for the pipeline is used.
     */
    const val ZAC_DEFAULT_DOCKER_IMAGE = "ghcr.io/infonl/zaakafhandelcomponent:dev"

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
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE = "melding-evenement-organiseren-behandelen"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION = "Melding evenement organiseren behandelen"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_IDENTIFICATIE =
        "indienen-aansprakelijkstelling-door-derden-behandelen"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION =
        "Indienen aansprakelijkstelling door derden behandelen"

    @Suppress("MagicNumber")
    val DATE_TIME_2000_01_01: ZonedDateTime = LocalDate.of(2000, Month.JANUARY, 1)
        .atStartOfDay(TimeZone.getDefault().toZoneId())

    @Suppress("MagicNumber")
    val DATE_TIME_2020_01_01: ZonedDateTime = LocalDate.of(2020, Month.JANUARY, 1)
        .atStartOfDay(TimeZone.getDefault().toZoneId())
    val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID: UUID = UUID.fromString("448356ff-dcfb-4504-9501-7fe929077c4f")
    val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID: UUID =
        UUID.fromString("fd2bf643-c98a-4b00-b2b3-9ae0c41ed425")
    val START_DATE = LocalDateTime.now()

    /**
     * Global variable to store the id of a task that is created in the integration tests.
     */
    lateinit var task1ID: String

    /**
     * Global variable to store the UUID of a zaak that is created in the integration tests
     * from a productaanvraag.
     */
    lateinit var zaakProductaanvraag1Uuid: UUID

    /**
     * Global variable to store the UUID of a zaak that is created in the integration tests
     * from a productaanvraag.
     */
    lateinit var zaakProductaanvraag2Uuid: UUID

    /**
     * Global variable to store the UUID of an uploaded file in the integration tests.
     */
    lateinit var enkelvoudigInformatieObjectUUID: String
}
