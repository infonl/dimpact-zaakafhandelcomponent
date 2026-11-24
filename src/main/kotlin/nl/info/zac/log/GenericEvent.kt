/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

class GenericEvent(
    val type: Type
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        AANVRAAG_PDF_MISSING("No PDF found in productaanvraag to link to the zaak"),
        EXCEPTION_RESPONSE_CONVERSION_FAILED("Failed to convert exception to JSON"),
        INITIATOR_KVK_ADDRESS_READ_FAILED("Could not read the KVK address of the initiator, so the mail is sent without it"),
        JOB_EVENT_HANDLING_FAILED("Asynchronous guard: handling a job event failed"),
        KEYCLOAK_UNREACHABLE_FOR_EMPLOYEE_LOOKUP(
            "Cannot connect to Keycloak to look up the Keycloak user ID of an employee; " +
                "employees are logged as unknown for 1 minute"
        ),
        JSON_READ_FAILED("JSON exception while reading from reader"),
        KVK_PROFIEL_READ_FAILED("Could not read the KVK profiel of the bedrijf, so its address is left out"),
        KVK_SEARCH_FAILED("Failed to search for company information using the KVK API"),
        REINDEXING_FAILED_UNEXPECTEDLY("Unexpected failure while reindexing"),
        SCREEN_EVENT_HANDLING_FAILED("Asynchronous guard: handling a screen event failed"),
        SCREEN_EVENT_JSON_CONVERSION_FAILED("Failed to convert the ScreenUpdateEvent to JSON"),
        SENSITIVE_STORAGE_CLEARED("Sensitive storage cleared"),
        SMTP_AUTHENTICATION_DISABLED("SMTP authentication disabled"),
        SUBSCRIPTION_MESSAGE_UNDECODABLE("SubscriptionMessage cannot be decrypted"),
        TEST_NOTIFICATION_RECEIVED("Received test callback URL status notification"),
        UNSUPPORTED_NIET_NATUURLIJK_PERSOON_IDENTIFICATIE("Unsupported niet-natuurlijk persoon identificatie"),
        WEB_SOCKET_HEARTBEAT_SCHEDULER_STARTING("WebSocket heartbeat scheduler starting"),
        ZTC_CACHE_REFRESHER_FAILED_UNEXPECTEDLY("Unexpected failure while refreshing the ZTC caches"),
        ZTC_CACHES_REFRESH_FAILED("Failed to refresh the ZTC caches"),
        ZTC_CACHES_REFRESH_STARTED("Started refreshing the ZTC caches"),
        ZTC_CACHES_WRITTEN("Wrote the refreshed types to the ZTC caches, except the ones invalidated during the refresh")
    }
}
