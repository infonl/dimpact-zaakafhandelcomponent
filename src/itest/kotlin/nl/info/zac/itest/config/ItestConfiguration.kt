/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
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

    /**
     * Temporarily increase the HTTP read timeout to 60 seconds to allow for
     * the slow 'document-creation/create-document-attended' endpoint to complete on slower computers.
     * In the long run, we should change this endpoint to be asynchronous.
     */
    const val HTTP_READ_TIMEOUT_SECONDS = 60L

    const val ACTIE_INTAKE_AFRONDEN = "INTAKE_AFRONDEN"
    const val ACTIE_ZAAK_AFHANDELEN = "ZAAK_AFHANDELEN"

    /**
     * Fake additional allowed file types just for testing purposes.
     */
    const val ADDITIONAL_ALLOWED_FILE_TYPES = "fakeFileExtension1,fakeFileExtension2"
    const val BAG_MOCK_BASE_URI = "http://bag-wiremock.local:8080"
    const val BAG_TEST_ADRES_1_IDENTIFICATION = "0363200003761447"
    const val BETROKKENE_TYPE_NATUURLIJK_PERSOON = "NATUURLIJK_PERSOON"
    const val BETROKKENE_IDENTIFICATION_TYPE_BSN = "BSN"
    const val BETROKKENE_IDENTIFACTION_TYPE_VESTIGING = "VN"
    const val BETROKKENE_ROL_TOEVOEGEN_REDEN = "Toegekend door de medewerker tijdens het behandelen van de zaak"
    const val BRON_ORGANISATIE = "123443210"
    const val CONFIG_MAX_FILE_SIZE_IN_MB = 80L
    const val CONFIG_GEMEENTE_CODE = "9999"
    const val CONFIG_GEMEENTE_NAAM = "FakeZacGemeente"
    const val COMMUNICATIEKANAAL_TEST_1 = "fakeCommunicatiekanaal1"
    const val COMMUNICATIEKANAAL_TEST_2 = "fakeCommunicatiekanaal2"
    const val DOMEIN_TEST_1 = "domein_test_1"
    const val FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE = "AANVULLENDE_INFORMATIE"
    const val HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM = "Aanvullende informatie"
    const val HUMAN_TASK_TYPE = "HUMAN_TASK"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING = "bijlage"
    const val INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID = "b1933137-94d6-49bc-9e12-afe712512276"
    const val INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING = "factuur"
    const val INFORMATIE_OBJECT_TYPE_FACTUUR_UUID = "eca3ae33-c9f1-4136-a48a-47dc3f4aaaf5"
    const val INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING = "e-mail"
    const val KEYCLOAK_HOSTNAME_URL = "http://localhost:8081"
    const val KEYCLOAK_HEALTH_READY_URL = "http://localhost:9001/health/ready"
    const val KEYCLOAK_REALM = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT = "zaakafhandelcomponent"
    const val KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"
    const val KVK_MOCK_BASE_URI = "http://kvk-wiremock:8080"
    const val OBJECT_PRODUCTAANVRAAG_1_UUID = "9dbed186-89ca-48d7-8c6c-f9995ceb8e27"
    const val OBJECT_PRODUCTAANVRAAG_2_UUID = "f1f6f670-fda8-4e98-81a6-6528937f10ee"
    const val OBJECT_PRODUCTAANVRAAG_BPMN_UUID = "fb6b2c0e-f745-4725-ae27-2317f0cfbfc4"
    const val OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK = "f8534f13-0669-4d4d-a364-6b6c4ad3d243"
    const val OBJECT_PRODUCTAANVRAAG_BPMN_BRON_KENMERK = "c7e9e087-853d-4b16-9750-fddd7c0b9b0d"
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
    const val PABC_CLIENT_BASE_URI = "http://pabc-api:8000"
    const val PABC_API_KEY = "zac-test-api-key"
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
    const val REFERENCE_TABLE_BRP_DOELBINDING_ZOEK_WAARDE_CODE = "BRP_DOELBINDING_ZOEK_WAARDE"
    const val REFERENCE_TABLE_BRP_DOELBINDING_ZOEK_WAARDE_NAAM = "BRP Doelbinding Zoekwaarde"
    const val REFERENCE_TABLE_BRP_DOELBINDING_RAADPLEEG_WAARDE_CODE = "BRP_DOELBINDING_RAADPLEEG_WAARDE"
    const val REFERENCE_TABLE_BRP_DOELBINDING_RAADPLEEG_WAARDE_NAAM = "BRP Doelbinding Raadpleegwaarde"
    const val RESULTAAT_TYPE_GEWEIGERD_UUID = "dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6"
    const val ROLTYPE_NAME_BELANGHEBBENDE = "Belanghebbende"
    const val ROLTYPE_NAME_MEDEAANVRAGER = "Medeaanvrager"
    const val ROLTYPE_UUID_BELANGHEBBENDE = "4c4cd850-8332-4bb9-adc4-dd046f0614ad"
    const val ROLTYPE_UUID_MEDEAANVRAGER = "b14cf056-0480-4060-a376-1dd522a50431"
    const val ROLTYPE_COUNT = 32
    const val SCREEN_EVENT_TYPE_TAKEN_VERDELEN = "TAKEN_VERDELEN"
    const val SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN = "TAKEN_VRIJGEVEN"
    const val SCREEN_EVENT_TYPE_ZAKEN_VERDELEN = "ZAKEN_VERDELEN"
    const val SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN = "ZAKEN_VRIJGEVEN"
    const val SCREEN_EVENT_TYPE_ZAAK_ROLLEN = "ZAAK_ROLLEN"

    const val TEST_GEMEENTE_EMAIL_ADDRESS = "gemeente-zac-test@example.com"
    const val TEST_INFORMATIE_OBJECT_TYPE_1_UUID = "efc332f2-be3b-4bad-9e3c-49a6219c92ad"

    const val TEST_SPEC_ORDER_INITIAL = 0
    const val TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED = 1
    const val TEST_SPEC_ORDER_AFTER_INITIALIZATION = 2
    const val TEST_SPEC_ORDER_AFTER_ZAAK_CREATED = 3
    const val TEST_SPEC_ORDER_AFTER_TASK_CREATED = 4
    const val TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED = 5
    const val TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED = 6
    const val TEST_SPEC_ORDER_AFTER_TASK_COMPLETED = 7
    const val TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED = 8
    const val TEST_SPEC_ORDER_AFTER_REINDEXING = 9
    const val TEST_SPEC_ORDER_AFTER_SEARCH = 10
    const val TEST_SPEC_ORDER_AFTER_KOPPELEN = 11

    const val TOTAL_COUNT_INDEXED_ZAKEN = 12
    const val TOTAL_COUNT_INDEXED_ZAKEN_AFGEROND = 2
    const val TOTAL_COUNT_INDEXED_TASKS = 4
    const val TOTAL_COUNT_INDEXED_DOCUMENTS = 11

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
        "Street # 38 & House # 10, Baghdad, Park Al-Sadoum, Hay Al-Nidhal 103, Irak"
    const val TEST_PERSON_2_BSN = "999992958"
    const val TEST_PERSON_3_BSN = "999991838"
    const val TEST_PDF_FILE_NAME = "fäkeTestDocument.pdf"
    const val TEST_PDF_FILE_SIZE = 9268
    const val TEST_TXT_FILE_NAME = "tëstTextDocument.txt"
    const val TEST_TXT_CONVERTED_TO_PDF_FILE_NAME = "tëstTextDocument.pdf"
    const val TEST_TXT_FILE_SIZE = 62
    const val TEST_VESTIGING_EMAIL = "fake.vestiging@example.com"
    const val TEST_VESTIGING_TELEPHONE_NUMBER = "0201234567"
    const val TEST_WORD_FILE_NAME = "fakeWordDocument.docx"
    const val TEST_USER_1_USERNAME = "testuser1"
    const val TEST_USER_1_PASSWORD = "testuser1"
    const val TEST_USER_1_EMAIL = "testuser1@example.com"
    const val TEST_USER_1_NAME = "Test User1 Špëçîâl Characters"
    const val TEST_USER_2_ID = "testuser2"
    const val FUNCTIONELE_GEBRUIKER_ID = "Functionele gebruiker"

    /**
     * Test user 2 does not have a first name, so their full name should be equal to their last name.
     */
    const val TEST_USER_2_NAME = "User2"
    const val TEST_USER_DOMEIN_TEST_1_USERNAME = "testuserdomeintest1"
    const val TEST_USER_DOMEIN_TEST_1_PASSWORD = "testuserdomeintest1"
    const val TEST_USER_DOMEIN_TEST_1_NAME = "Test Testuserdomeintest1"
    const val TEST_FUNCTIONAL_ADMIN_1_USERNAME = "functioneelbeheerder1"
    const val TEST_FUNCTIONAL_ADMIN_1_NAME = "Test Functioneelbeheerder1"
    const val TEST_RECORD_MANAGER_1_USERNAME = "recordmanager1"
    const val TEST_RECORD_MANAGER_1_NAME = "Test Recordmanager1"
    const val TEST_COORDINATOR_1_USERNAME = "coordinator1"
    const val TEST_COORDINATOR_1_PASSWORD = "coordinator1"
    const val TEST_COORDINATOR_1_NAME = "Test Coordinator1"
    const val TEST_BEHANDELAAR_1_USERNAME = "behandelaar1"
    const val TEST_BEHANDELAAR_1_PASSWORD = "behandelaar1"
    const val TEST_BEHANDELAAR_1_NAME = "Test Behandelaar1"
    const val TEST_RAADPLEGER_1_USERNAME = "raadpleger1"
    const val TEST_RAADPLEGER_1_PASSWORD = "raadpleger1"
    const val TEST_RAADPLEGER_1_NAME = "Test Raadpleger1"
    const val TEST_USER_WITHOUT_ANY_ROLE_USERNAME = "userwithoutanyrole"
    const val TEST_USER_WITHOUT_ANY_ROLE_PASSWORD = "userwithoutanyrole"
    const val TEST_USER_WITHOUT_ANY_ROLE_NAME = "Test User Without Any Role"
    const val TEST_GROUP_A_ID = "test-group-a"
    const val TEST_GROUP_A_DESCRIPTION = "Test group A"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_ID = "test-group-fb"
    const val TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION = "Test group Functional Admins"
    const val TEST_GROUP_RECORD_MANAGERS_ID = "test-group-rm"
    const val TEST_GROUP_RECORD_MANAGERS_DESCRIPTION = "Test group Record Managers"
    const val TEST_GROUP_COORDINATORS_ID = "test-group-co"
    const val TEST_GROUP_COORDINATORS_DESCRIPTION = "Test group Coordinators"
    const val TEST_GROUP_BEHANDELAARS_ID = "test-group-bh"
    const val TEST_GROUP_BEHANDELAARS_DESCRIPTION = "Test group Behandelaars"
    const val TEST_GROUP_RAADPLEGERS_ID = "test-group-rp"
    const val TEST_GROUP_RAADPLEGERS_DESCRIPTION = "Test group Raadplegers"
    const val TEST_GROUP_DOMEIN_TEST_1_ID = "test-group-domein-test-1"
    const val TEST_GROUP_DOMEIN_TEST_1_DESCRIPTION = "Test group which has access to domein_test_1 only"
    const val TEST_GROUPS_ALL =
        """
            [
                {
                    "id": "$TEST_GROUP_FUNCTIONAL_ADMINS_ID",
                    "naam": "$TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION"
                },
                {
                    "id": "$TEST_GROUP_RECORD_MANAGERS_ID",
                    "naam": "$TEST_GROUP_RECORD_MANAGERS_DESCRIPTION"
                },
                {
                    "id": "$TEST_GROUP_COORDINATORS_ID",
                    "naam": "$TEST_GROUP_COORDINATORS_DESCRIPTION"
                },
                {
                    "id": "$TEST_GROUP_BEHANDELAARS_ID",
                    "naam": "$TEST_GROUP_BEHANDELAARS_DESCRIPTION"
                },
                {
                    "id": "$TEST_GROUP_RAADPLEGERS_ID",
                    "naam": "$TEST_GROUP_RAADPLEGERS_DESCRIPTION"
                },
                {
                    "id": "$TEST_GROUP_A_ID",
                    "naam": "$TEST_GROUP_A_DESCRIPTION"
                },
                {
                    "id": "$TEST_GROUP_DOMEIN_TEST_1_ID",
                    "naam": "$TEST_GROUP_DOMEIN_TEST_1_DESCRIPTION"
                }
            ]
        """

    /**
     * Constants used in the Informatieobjecten tests
     */
    const val DOCUMENT_FILE_TITLE = "fakeTitel"
    const val WORD_DOCUMENT_FILE_TITLE = "fakeWordTitel"
    const val DOCUMENT_UPDATED_FILE_TITLE = "updated title with Špëcîål characters"
    const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK = "ZAAKVERTROUWELIJK"
    const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR = "OPENBAAR"
    const val DOCUMENT_STATUS_DEFINITIEF = "definitief"
    const val DOCUMENT_STATUS_IN_BEWERKING = "in_bewerking"
    const val TEXT_MIME_TYPE = "application/text"
    const val PDF_MIME_TYPE = "application/pdf"
    val DOCUMENT_1_IDENTIFICATION = "DOCUMENT-${LocalDate.now().year}-0000000001"
    val DOCUMENT_2_IDENTIFICATION = "DOCUMENT-${LocalDate.now().year}-0000000002"
    val DOCUMENT_3_IDENTIFICATION = "DOCUMENT-${LocalDate.now().year}-0000000003"
    val DOCUMENT_4_IDENTIFICATION = "DOCUMENT-${LocalDate.now().year}-0000000004"

    /**
     * Constants used in the KVK WireMock template response
     */
    const val TEST_KVK_ADRES_1 = "fakeStraatnaam1"
    const val TEST_KVK_EERSTE_HANDELSNAAM_1 = "fakeEersteHandelsnaam1"
    const val TEST_KVK_NAAM_1 = "fakeNaam1"
    const val TEST_KVK_NUMMER_1 = "12345678"
    const val TEST_KVK_PLAATS_1 = "fakePlaats1"
    const val TEST_KVK_RSIN_1 = "123456789"
    const val TEST_KVK_VESTIGINGSNUMMER_1 = "000012345678"
    const val TEST_KVK_VESTIGINGSTYPE_HOOFDVESTIGING = "HOOFDVESTIGING"
    const val TEST_KVK_TYPE_RECHTSPERSOON = "RECHTSPERSOON"
    const val TEST_KVK_VESTIGING1_TOTAAL_WERKZAME_PERSONEN = 3
    const val TEST_KVK_VESTIGING1_VOLTIJD_WERKZAME_PERSONEN = 2
    const val TEST_KVK_VESTIGING1_HOOFDACTIVITEIT = "fakesbiOmschrijving1"
    const val TEST_KVK_VESTIGING1_NEVENACTIVITEIT1 = "fakesbiOmschrijving2"
    const val TEST_KVK_VESTIGING1_NEVENACTIVITEIT2 = "fakesbiOmschrijving3"

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

    const val SMART_DOCUMENTS_FILE_ID = "fakeFileId"
    const val SMART_DOCUMENTS_FILE_TITLE = "Smart Documents file"

    const val SMTP_SERVER_PORT = 25
    const val TAAK_1_FATAL_DATE = "1970-01-17"
    const val VERANTWOORDELIJKE_ORGANISATIE = "316245124"
    const val VESTIGINGTYPE_NEVENVESTIGING = "NEVENVESTIGING"
    const val ZAC_INTERNAL_ENDPOINTS_API_KEY = "zacItestInternalEndpointsApiKey"

    const val ZAAK_DESCRIPTION_1 = "fakeZaakDescription1"
    const val ZAAK_DESCRIPTION_2 = "fakeZaakDescription2"
    const val ZAAK_EXPLANATION_1 = "fakeZaakExplanation1"
    const val ZAAK_OMSCHRIJVING = "fakeOmschrijving"

    /**
     * First zaak created from a productaanvraag.
     * Currently, the zaak identification is not set by ZAC but generated by OpenZaak using the format:
     * 'ZAAK-YYYY-SEQUENCE_NUMBER' where the year is taken from the start date of the zaak.
     */
    const val ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION = "ZAAK-1970-0000000001"
    const val ZAAK_PRODUCTAANVRAAG_1_START_DATE = "1970-01-01"
    const val ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING = "1970-01-15"
    const val ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING = "fakeZaakOmschrijving"
    const val ZAAK_PRODUCTAANVRAAG_1_TOELICHTING = "fakeZaakToelichting"

    /**
     * Second zaak created from a productaanvraag.
     * Currently, the zaak identification is not set by ZAC but generated by OpenZaak using the format:
     * 'ZAAK-YYYY-SEQUENCE_NUMBER' where the year is taken from the start date of the zaak.
     */
    const val ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION = "ZAAK-1999-0000000001"
    const val ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE = "2023-10-30"
    const val ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_TITEL = "Fake test document"
    const val ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_FILE_NAME = "fake-test-document.pdf"

    const val ZAAK_PRODUCTAANVRAAG_BPMN_IDENTIFICATION = "ZAAK-1998-0000000001"
    const val ZAAK_PRODUCTAANVRAAG_BPMN_UITERLIJKE_EINDDATUM_AFDOENING = "1998-01-31"

    const val ZAAK_MANUAL_2000_03_IDENTIFICATION = "ZAAK-2000-0000000003"
    const val ZAAK_MANUAL_2020_01_IDENTIFICATION = "ZAAK-2020-0000000001"
    const val ZAAK_MANUAL_2024_01_IDENTIFICATION = "ZAAK-2024-0000000001"

    const val ZAC_CONTAINER_SERVICE_NAME = "zac"

    /**
     * The default ZAC Docker image used when running the integration tests locally.
     * When running the tests in our GitHub pipeline, a different Docker image specific for the pipeline is used.
     */
    const val ZAC_DEFAULT_DOCKER_IMAGE = "ghcr.io/infonl/zaakafhandelcomponent:dev"

    const val ZAC_BASE_URI = "http://localhost:$ZAC_CONTAINER_PORT"

    /**
     * The ZAC API URI from outside the Docker network.
     */
    const val ZAC_API_URI = "$ZAC_BASE_URI/rest"

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

    /**
     * Zaak beeindig constants
     */
    const val ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_ID = "-1"
    const val ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_NAME = "Verzoek is door initiator ingetrokken"
    const val ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_ID = "-2"
    const val ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_NAME = "Zaak is een duplicaat"
    const val ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_ID = "-3"
    const val ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_NAME =
        "Verzoek is bij verkeerde organisatie ingediend"

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

    @Suppress("MagicNumber")
    val DATE_2025_07_01: LocalDate = LocalDate.of(2025, Month.JULY, 1)

    @Suppress("MagicNumber")
    val DATE_2025_01_01: LocalDate = LocalDate.of(2025, Month.JANUARY, 1)

    val DATE_TIME_2000_01_01: ZonedDateTime = DATE_2000_01_01.atStartOfDay(TimeZone.getDefault().toZoneId())
    val DATE_TIME_2020_01_01: ZonedDateTime = DATE_2020_01_01.atStartOfDay(TimeZone.getDefault().toZoneId())
    val DATE_TIME_2024_01_01: ZonedDateTime = DATE_2024_01_01.atStartOfDay(TimeZone.getDefault().toZoneId())
    val DATE_TIME_2024_01_31: ZonedDateTime = DATE_2024_01_31.atStartOfDay(TimeZone.getDefault().toZoneId())

    val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID: UUID = UUID.fromString("448356ff-dcfb-4504-9501-7fe929077c4f")
    val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID: UUID =
        UUID.fromString("fd2bf643-c98a-4b00-b2b3-9ae0c41ed425")
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE = "melding-evenement-organiseren-behandelen"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION = "Melding evenement organiseren behandelen"
    const val ZAAKTYPE_MELDING_KLEIN_EVENEMENT_REFERENTIEPROCES = "melding klein evenement"

    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE =
        "indienen-aansprakelijkstelling-behandelen"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION =
        "Indienen aansprakelijkstelling door derden behandelen"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BELANGHEBBENDE = "3bb6928b-76de-4716-ac5f-fa3d7d6eca36"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BEWINDVOERDER = "966ddb36-6989-4635-8a37-d7af980a37a6"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_CONTACTPERSOON = "ca31355e-abbf-4675-8700-9d167b194db1"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_GEMACHTIGDE = "4b473a85-5516-441f-8d7d-57512c6b6833"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_MEDEAANVRAGER = "e49a634b-731c-4460-93f4-e919686811aa"
    const val ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_PLAATSVERVANGER =
        "74799b20-0350-457d-8773-a0f1ab16b299"

    val START_DATE: LocalDateTime = LocalDateTime.now()

    /**
     * BPMN tests constants
     */
    val ZAAKTYPE_BPMN_TEST_UUID: UUID = UUID.fromString("26076928-ce07-4d5d-8638-c2d276f6caca")
    const val ZAAK_BPMN_TEST_IDENTIFICATION: String = "ZAAK-2000-0000000004"
    const val ZAAKTYPE_BPMN_TEST_IDENTIFICATIE = "bpmn-test-zaaktype"
    const val ZAAKTYPE_BPMN_TEST_DESCRIPTION = "BPMN test zaaktype"
    const val ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE = "bpmn-test-productaanvraagtype"

    val ZAAKTYPE_BPMN_EVENEMENTEN_VOOROVERLEG_UUID: UUID = UUID.fromString("8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a")
    const val ZAAKTYPE_BPMN_EVENEMENTEN_VOOROVERLEG_IDENTIFICATIE = "bpmn-evenementen-vooroverleg"
    const val ZAAKTYPE_BPMN_EVENEMENTEN_VOOROVERLEG_DESCRIPTION = "BPMN Evenementen Vooroverleg"

    const val BPMN_TEST_PROCESS_ID = "itProcessDefinition"
    const val BPMN_TEST_PROCESS_RESOURCE_PATH = "bpmn/$BPMN_TEST_PROCESS_ID.bpmn"
    const val BPMN_TEST_FORM_RESOURCE_PATH = "bpmn/testForm.json"
    const val BPMN_SUMMARY_FORM_RESOURCE_PATH = "bpmn/summaryForm.json"
    const val BPMN_TEST_TASK_NAME = "Test"
    const val BPMN_SUMMARY_TASK_NAME = "Summary"
    const val BPMN_TEST_FORM_NAME = "Test form"
    const val BPMN_SUMMARY_FORM_NAME = "Summary form"

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
     * Global variable to store the UUID of a BPMN zaak that is created in the integration tests
     * from a productaanvraag.
     */
    lateinit var zaakProductaanvraag3Uuid: UUID

    /**
     * Global variable to store the UUID of an uploaded file in the integration tests.
     */
    lateinit var enkelvoudigInformatieObjectUUID: String

    lateinit var zaakProductaanvraag1Betrokkene1Uuid: UUID
}
