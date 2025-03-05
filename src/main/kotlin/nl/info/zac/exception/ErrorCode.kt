/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.exception

/**
* ZAC application backend error codes used in exception messages.
* These should be translated by the frontend to human-readable messages.
*/
enum class ErrorCode(val value: String) {
    ERROR_CODE_BAG_CLIENT("msg.error.bag.client.exception"),
    ERROR_CODE_BETROKKENE_WAS_ALREADY_ADDED_TO_ZAAK("msg.error.betrokkene.was.already.added.to.zaak"),
    ERROR_CODE_BRC_CLIENT("msg.error.brc.client.exception"),
    ERROR_CODE_BRP_CLIENT("msg.error.brp.client.exception"),
    ERROR_CODE_DRC_CLIENT("msg.error.drc.client.exception"),
    ERROR_CODE_FORBIDDEN("msg.error.server.forbidden"),
    ERROR_CODE_SERVER_GENERIC("msg.error.server.generic"),
    ERROR_CODE_KLANTINTERACTIES_CLIENT("msg.error.klanten.client.exception"),
    ERROR_CODE_OBJECTS_CLIENT("msg.error.objects.client.exception"),
    ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE("msg.error.productaanvraagtype.already.in.use"),
    ERROR_CODE_REFERENCE_TABLE_SYSTEM_VALUES_CANNOT_BE_CHANGED(
        "msg.error.system.reference.table.system.values.cannot.be.changed"
    ),
    ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS("msg.error.reference.table.with.same.code.already.exists"),
    ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKAFHANDELPARAMETERS(
        "msg.error.reference.table.is.in.use.by.zaakafhandelparameters"
    ),
    ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED("msg.error.system.reference.table.cannot.be.deleted"),
    ERROR_CODE_ZRC_CLIENT("msg.error.zrc.client.exception"),
    ERROR_CODE_ZTC_CLIENT("msg.error.ztc.client.exception"),
    ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED("msg.error.smartdocuments.not.configured"),
    ERROR_CODE_SMARTDOCUMENTS_DISABLED("msg.error.smartdocuments.disabled"),
    ERROR_CODE_VALIDATION_GENERIC("msg.error.validation.generic"),
    ERROR_CODE_BESLUIT_PUBLICATION_DISABLED_TYPE("msg.error.besluit.publication.disabled"),
    ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE("msg.error.besluit.publication.date.missing"),
    ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE("msg.error.besluit.response.date.missing"),
    ERROR_CODE_BESLUIT_RESPONSE_DATE_INVALID_TYPE("msg.error.besluit.response.date.invalid"),
    ERROR_CODE_CASE_HAS_OPEN_SUBCASES("msg.error.case.has.open.subcases"),
    ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS("msg.error.case.has.locked.information.objects"),
    ERROR_CODE_SEARCH_INDEXING("msg.error.search.indexing.exception"),
    ERROR_CODE_SEARCH_SEARCH("msg.error.search.search.exception"),

    ERROR_CODE_USER_NOT_IN_GROUP("msg.error.user.not.in.group"),
    ERROR_CODE_USER_NOT_FOUND_IN_KEYCLOAK("msg.error.user.not.in.keycloak"),
    ERROR_CODE_GROUP_NOT_FOUND_IN_KEYCLOAK("msg.error.group.not.in.keycloak"),
}
