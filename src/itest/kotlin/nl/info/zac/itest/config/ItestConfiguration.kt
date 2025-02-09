/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

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
    private const val GREENMAIL_API_PORT = 18083

    const val HTTP_STATUS_OK = 200
    const val HTTP_STATUS_NO_CONTENT = 204
    const val HTTP_STATUS_SEE_OTHER = 303
    const val HTTP_STATUS_BAD_REQUEST = 400
    const val HTTP_STATUS_FORBIDDEN = 403
    const val HTTP_STATUS_NOT_FOUND = 404

    /**
     * Temporarily increase the HTTP read timeout to 60 seconds to allow for
     * the slow 'document-creation/create-document-attended' endpoint to complete on slower computers.
     * In the long run we should change this endpoint to be asynchronous.
     */
    const val HTTP_READ_TIMEOUT_SECONDS = 60L

    const val ACTIE_INTAKE_AFRONDEN = "INTAKE_AFRONDEN"
    const val ACTIE_ZAAK_AFHANDELEN = "ZAAK_AFHANDELEN"
    const val BAG_MOCK_BASE_URI = "http://bag-wiremock.local:8080"
    const val BAG_TEST_ADRES_1_IDENTIFICATION = "0363200003761447"
    const val BETROKKENE_TYPE_NATUURLIJK_PERSOON = "NATUURLIJK_PERSOON"
    const val BETROKKENE_IDENTIFICATION_TYPE_BSN = "BSN"
    const val BETROKKENE_IDENTIFACTION_TYPE_VESTIGING = "VN"
    const val BETROKKENE_ROL_TOEVOEGEN_REDEN = "Toegekend door de medewerker tijdens het behandelen van de zaak"
    const val BRON_ORGANISATIE = "123443210"
    const val CONFIG_MAX_FILE_SIZE_IN_MB = 80L
    const val CONFIG_GEMEENTE_CODE = "9999"
    const val CONFIG_GEMEENTE_NAAM = "DummyZacGemeente"
    const val COMMUNICATIEKANAAL_TEST_1 = "dummyCommunicatiekanaal1"
    const val COMMUNICATIEKANAAL_TEST_2 = "dummyCommunicatiekanaal2"
    const val FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE = "AANVULLENDE_INFORMATIE"
    const val HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM = "Aanvullende informatie"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING = "bijlage"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID = "b1933137-94d6-49bc-9e12-afe712512276"
    const val INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING = "e-mail"
    const val KEYCLOAK_HOSTNAME_URL = "http://localhost:8081"
    const val KEYCLOAK_HEALTH_READY_URL = "http://localhost:9001/health/ready"
    const val KEYCLOAK_REALM = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"
    const val KVK_MOCK_BASE_URI = "http://kvk-wiremock:8080"
    const val OBJECT_PRODUCTAANVRAAG_1_UUID = "9dbed186-89ca-48d7-8c6c-f9995ceb8e27"
    const val OBJECT_PRODUCTAANVRAAG_2_UUID = "f1f6f670-fda8-4e98-81a6-6528937f10ee"
    const val OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK = "f8534f13-0669-4d4d-a364-6b6c4ad3d243"
    const val OBJECTS_BASE_URI = "http://objecten-api.local:8000"
    const val OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT = "021f685e-9482-4620-b157-34cd4003da6b"
    const val OFFICE_CONVERTER_BASE_URI = "http://office-converter:8080"
    const val OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK = "dca40822-5eb3-4acc-b915-7b020041ad55"
    const val OPEN_FORMULIEREN_FORMULIER_BRON_NAAM = "open-forms"
    const val OPEN_NOTIFICATIONS_API_SECRET_KEY = "openNotificatiesApiSecretKey"
    const val OPEN_ZAAK_BASE_URI = "http://openzaak.local:8000"
    const val OPEN_ZAAK_EXTERNAL_PORT = 8001
    const val OPEN_ZAAK_EXTERNAL_URI = "http://localhost:$OPEN_ZAAK_EXTERNAL_PORT"
    const val OPEN_ZAAK_CLIENT_ID = "zac_client"
    const val OPEN_ZAAK_CLIENT_SECRET = "openzaakZaakafhandelcomponentClientSecret"
    const val PRODUCTAANVRAAG_TYPE_1 = "productaanvraag-type-1"
    const val PRODUCTAANVRAAG_TYPE_2 = "productaanvraag-type-2"
    const val PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LATITUDE = 52.08968250760225
    const val PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LONGITUDE = 5.114358701512936
    const val REFERENCE_TABLE_ADVIES_CODE = "ADVIES"
    const val REFERENCE_TABLE_ADVIES_NAME = "Advies"
    const val REFERENCE_TABLE_AFZENDER_CODE = "AFZENDER"
    const val REFERENCE_TABLE_AFZENDER_NAME = "Afzender"
    const val REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE = "COMMUNICATIEKANAAL"
    const val REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME = "Communicatiekanaal"
    const val REFERENCE_TABLE_DOMEIN_CODE = "DOMEIN"
    const val REFERENCE_TABLE_DOMEIN_NAME = "Domein"
    const val REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE = "SERVER_ERROR_ERROR_PAGINA_TEKST"
    const val REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_NAME = "Server error error pagina tekst"
    const val ROLTYPE_NAME_BELANGHEBBENDE = "Belanghebbende"
    const val ROLTYPE_NAME_MEDEAANVRAGER = "Medeaanvrager"
    const val ROLTYPE_UUID_BELANGHEBBENDE = "4c4cd850-8332-4bb9-adc4-dd046f0614ad"
    const val ROLTYPE_UUID_MEDEAANVRAGER = "b14cf056-0480-4060-a376-1dd522a50431"
    const val ROLTYPE_COUNT = 16
    const val SCREEN_EVENT_TYPE_TAKEN_VERDELEN = "TAKEN_VERDELEN"
    const val SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN = "TAKEN_VRIJGEVEN"
    const val SCREEN_EVENT_TYPE_ZAKEN_VERDELEN = "ZAKEN_VERDELEN"
    const val SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN = "ZAKEN_VRIJGEVEN"
    const val SCREEN_EVENT_TYPE_ZAAK_ROLLEN = "ZAAK_ROLLEN"

    const val TEST_GEMEENTE_EMAIL_ADDRESS = "gemeente-zac-test@example.com"

    const val TEST_INFORMATIE_OBJECT_TYPE_1_UUID = "efc332f2-be3b-4bad-9e3c-49a6219c92ad"

    const val TEST_SPEC_ORDER_INITIAL = 0
    const val TEST_SPEC_ORDER_AFTER_ZAAK_CREATED = 1
    const val TEST_SPEC_ORDER_AFTER_TASK_CREATED = 2
    const val TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED = 3
    const val TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED = 4
    const val TEST_SPEC_ORDER_AFTER_TASK_COMPLETED = 5
    const val TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED = 6
    const val TEST_SPEC_ORDER_AFTER_REINDEXING = 7
    const val TEST_SPEC_ORDER_AFTER_SEARCH = 8
    const val TEST_SPEC_ORDER_LAST = 100

    const val TOTAL_COUNT_ZAKEN = 10
    const val TOTAL_COUNT_ZAKEN_AFGEROND = 2
    const val TOTAL_COUNT_TASKS = 2
    const val TOTAL_COUNT_DOCUMENTS = 7

    /**
     * Test person that exists in both the BRP and the Klanten API databases
     */
    const val TEST_PERSON_HENDRIKA_JANSE_BSN = "999993896"
    const val TEST_PERSON_HENDRIKA_JANSE_EMAIL = "hendrika.janse@example.com"
    const val TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE = "1965-01-01"
    const val TEST_PERSON_HENDRIKA_JANSE_GENDER = "vrouw"
    const val TEST_PERSON_HENDRIKA_JANSE_FULLNAME = "Héndrika Janse"
    const val TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER = "0612345678"
    const val TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE =
        "Street # 38 & House # 10, Baghdad, Park Al-Sadoum, Hay Al-Nidhal 103"
    const val TEST_PERSON_2_BSN = "999992958"
    const val TEST_PERSON_3_BSN = "999991838"
    const val TEST_PDF_FILE_NAME = "dummyTestDocument.pdf"
    const val TEST_PDF_FILE_SIZE = 9268
    const val TEST_TXT_FILE_NAME = "testTextDocument.txt"
    const val TEST_TXT_CONVERTED_TO_PDF_FILE_NAME = "testTextDocument.pdf"
    const val TEST_TXT_FILE_SIZE = 63
    const val TEST_USER_1_USERNAME = "testuser1"
    const val TEST_USER_1_PASSWORD = "testuser1"
    const val TEST_USER_1_EMAIL = "testuser1@example.com"
    const val TEST_USER_1_NAME = "Test User1 Špëçîâl Characters"
    const val TEST_USER_2_ID = "testuser2"

    /**
     * Test user 2 does not have a first name so their full name should be equal to their last name.
     */
    const val TEST_USER_2_NAME = "User2"
    const val TEST_USER_DOMEIN_TEST_1_ID = "testuserdomeintest1"
    const val TEST_USER_DOMEIN_TEST_1_NAME = "Test Testuserdomeintest1"
    const val TEST_RECORD_MANAGER_1_USERNAME = "recordmanager1"
    const val TEST_RECORD_MANAGER_1_NAME = "Test Recordmanager1"
    const val TEST_FUNCTIONAL_ADMIN_1_ID = "functioneelbeheerder1"
    const val TEST_FUNCTIONAL_ADMIN_1_NAME = "Test Functioneelbeheerder1"
    const val TEST_GROUP_A_ID = "test-group-a"
    const val TEST_GROUP_A_DESCRIPTION = "Test group A"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_ID = "test-group-fb"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION = "Test group functional admins"
    const val TEST_GROUP_DOMEIN_TEST_1_ID = "test-group-domein-test-1"
    const val TEST_GROUP_DOMEIN_TEST_1_DESCRIPTION = "Test group which has access to domein_test_1 only"
    const val TEST_GROUP_RECORD_MANAGERS_ID = "test-group-rm"
    const val TEST_GROUP_RECORD_MANAGERS_DESCRIPTION = "Test group record managers"

    /**
     * Constants used in the Informatieobjecten tests
     */
    const val DOCUMENT_FILE_TITLE = "dummyTitel"
    const val DOCUMENT_UPDATED_FILE_TITLE = "updated title with Špëcîål characters"
    const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK = "ZAAKVERTROUWELIJK"
    const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR = "OPENBAAR"
    const val DOCUMENT_STATUS_DEFINITIEF = "definitief"
    const val DOCUMENT_STATUS_IN_BEWERKING = "in_bewerking"
    const val TEXT_MIME_TYPE = "application/text"
    const val PDF_MIME_TYPE = "application/pdf"
    val DOCUMENT_1_IDENTIFICATION = "DOCUMENT-${LocalDate.now().year}-0000000001"
    val DOCUMENT_2_IDENTIFICATION = "DOCUMENT-${LocalDate.now().year}-0000000002"

    /**
     * Constants used in the KVK WireMock template response
     */
    const val TEST_KVK_ADRES_1 = "dummyStraatnaam1"
    const val TEST_KVK_EERSTE_HANDELSNAAM_1 = "dummyEersteHandelsnaam1"
    const val TEST_KVK_NAAM_1 = "dummyNaam1"
    const val TEST_KVK_NUMMER_1 = "12345678"
    const val TEST_KVK_PLAATS_1 = "dummyPlaats1"
    const val TEST_KVK_RSIN_1 = "123456789"
    const val TEST_KVK_VESTIGINGSNUMMER_1 = "000012345678"
    const val TEST_KVK_VESTIGINGSTYPE_HOOFDVESTIGING = "HOOFDVESTIGING"
    const val TEST_KVK_VESTIGING1_TOTAAL_WERKZAME_PERSONEN = 3
    const val TEST_KVK_VESTIGING1_VOLTIJD_WERKZAME_PERSONEN = 2
    const val TEST_KVK_VESTIGING1_HOOFDACTIVITEIT = "dummysbiOmschrijving1"
    const val TEST_KVK_VESTIGING1_NEVENACTIVITEIT1 = "dummysbiOmschrijving2"
    const val TEST_KVK_VESTIGING1_NEVENACTIVITEIT2 = "dummysbiOmschrijving3"

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

    const val SMART_DOCUMENTS_FILE_ID = "dummyFileId"
    const val SMART_DOCUMENTS_FILE_TITLE = "Smart Documents file"

    const val SMTP_SERVER_PORT = 25
    const val TAAK_1_FATAL_DATE = "1970-01-17"
    const val VERANTWOORDELIJKE_ORGANISATIE = "316245124"
    const val VESTIGINGTYPE_NEVENVESTIGING = "NEVENVESTIGING"
    const val ZAAK_DESCRIPTION_1 = "dummyZaakDescription1"
    const val ZAAK_DESCRIPTION_2 = "dummyZaakDescription2"
    const val ZAAK_EXPLANATION_1 = "dummyZaakExplanation1"

    /**
     * First zaak created from a productaanvraag.
     * Currently, the zaak identification is not set by ZAC but generated by OpenZaak using the format:
     * 'ZAAK-YYYY-SEQUENCE_NUMBER' where the year is taken from the start date of the zaak.
     */
    const val ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION = "ZAAK-1970-0000000001"
    const val ZAAK_PRODUCTAANVRAAG_1_START_DATE = "1970-01-01"
    const val ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING = "1970-01-15"
    const val ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING = "dummyZaakOmschrijving"
    const val ZAAK_PRODUCTAANVRAAG_1_TOELICHTING = "dummyZaakToelichting"

    /**
     * Second zaak created from a productaanvraag.
     * Currently, the zaak identification is not set by ZAC but generated by OpenZaak using the format:
     * 'ZAAK-YYYY-SEQUENCE_NUMBER' where the year is taken from the start date of the zaak.
     */
    const val ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION = "ZAAK-1999-0000000001"
    const val ZAAK_PRODUCTAANVRAAG_2_START_DATE = "1999-01-01"
    const val ZAAK_PRODUCTAANVRAAG_2_UITERLIJKE_EINDDATUM_AFDOENING = "1999-01-15"
    const val ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE = "2023-10-30"
    const val ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_TITEL = "Dummy test document"
    const val ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_FILE_NAME = "dummy-test-document.pdf"

    /**
     * First 'manually' created zaak using the ZAC API.
     */
    const val ZAAK_MANUAL_1_IDENTIFICATION = "ZAAK-2020-0000000001"
    const val ZAAK_MANUAL_2_IDENTIFICATION = "ZAAK-2000-0000000001"

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
     * GreenMail API base URI for testing sent mail
     */
    const val GREENMAIL_API_URI = "http://localhost:$GREENMAIL_API_PORT/api"

    /**
     * The ZAC management URI from outside the Docker network.
     */
    const val ZAC_MANAGEMENT_URI = "http://localhost:$ZAC_MANAGEMENT_PORT"
    const val ZAC_HEALTH_READY_URL = "$ZAC_MANAGEMENT_URI/health/ready"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE = "melding-evenement-organiseren-behandelen"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION = "Melding evenement organiseren behandelen"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_REFERENTIEPROCES = "melding klein evenement"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_IDENTIFICATIE =
        "indienen-aansprakelijkstelling-door-derden-behandelen"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN = "indienen-aansprakelijkstelling-behandelen"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION =
        "Indienen aansprakelijkstelling door derden behandelen"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BELANGHEBBENDE = "3bb6928b-76de-4716-ac5f-fa3d7d6eca36"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BEWINDVOERDER = "966ddb36-6989-4635-8a37-d7af980a37a6"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_CONTACTPERSOON = "ca31355e-abbf-4675-8700-9d167b194db1"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_GEMACHTIGDE = "4b473a85-5516-441f-8d7d-57512c6b6833"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_MEDEAANVRAGER = "e49a634b-731c-4460-93f4-e919686811aa"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_PLAATSVERVANGER =
        "74799b20-0350-457d-8773-a0f1ab16b299"

    @Suppress("MagicNumber")
    val DATE_TIME_2000_01_01: ZonedDateTime = LocalDate.of(2000, Month.JANUARY, 1)
        .atStartOfDay(TimeZone.getDefault().toZoneId())

    @Suppress("MagicNumber")
    val DATE_2000_01_01: LocalDate = LocalDate.of(2000, Month.JANUARY, 1)

    @Suppress("MagicNumber")
    val DATE_2020_01_01: LocalDate = LocalDate.of(2020, Month.JANUARY, 1)

    @Suppress("MagicNumber")
    val DATE_2020_01_15: LocalDate = LocalDate.of(2020, Month.JANUARY, 15)

    @Suppress("MagicNumber")
    val DATE_2023_09_21: LocalDate = LocalDate.of(2023, Month.SEPTEMBER, 21)

    @Suppress("MagicNumber")
    val DATE_2023_10_01: LocalDate = LocalDate.of(2023, Month.OCTOBER, 1)

    @Suppress("MagicNumber")
    val DATE_2024_01_01: LocalDate = LocalDate.of(2024, Month.JANUARY, 1)

    @Suppress("MagicNumber")
    val DATE_2024_01_31: LocalDate = LocalDate.of(2024, Month.JANUARY, 31)

    val DATE_TIME_2020_01_01: ZonedDateTime = DATE_2020_01_01.atStartOfDay(TimeZone.getDefault().toZoneId())
    val DATE_TIME_2024_01_01: ZonedDateTime = DATE_2024_01_01.atStartOfDay(TimeZone.getDefault().toZoneId())

    val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID: UUID = UUID.fromString("448356ff-dcfb-4504-9501-7fe929077c4f")
    val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID: UUID =
        UUID.fromString("fd2bf643-c98a-4b00-b2b3-9ae0c41ed425")
    val START_DATE: LocalDateTime = LocalDateTime.now()

    /**
     * Global variable to store the id of a task that is created in the integration tests as
     * part of the zaak with UUID [zaakProductaanvraag1Uuid].
     */
    lateinit var task1ID: String

    /**
     * Second 'manually' created zaak using the ZAC API.
     */
    lateinit var zaakManual2Identification: String

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

    lateinit var zaakProductaanvraag1Betrokkene1Uuid: UUID
}
