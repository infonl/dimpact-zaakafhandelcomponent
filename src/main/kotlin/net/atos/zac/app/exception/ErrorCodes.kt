/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.exception

/**
* ZAC application backend error codes used in exception messages.
* These should be translated by the frontend to human-readable messages.
*/

const val ERROR_CODE_BAG_CLIENT = "msg.error.bag.client.exception"
const val ERROR_CODE_BETROKKENE_WAS_ALREADY_ADDED_TO_ZAAK = "msg.error.betrokkene.was.already.added.to.zaak"
const val ERROR_CODE_BRC_CLIENT = "msg.error.brc.client.exception"
const val ERROR_CODE_BRP_CLIENT = "msg.error.brp.client.exception"
const val ERROR_CODE_DRC_CLIENT = "msg.error.drc.client.exception"
const val ERROR_CODE_FORBIDDEN = "msg.error.server.forbidden"
const val ERROR_CODE_GENERIC_SERVER = "msg.error.server.generic"
const val ERROR_CODE_KLANTINTERACTIES_CLIENT = "msg.error.klanten.client.exception"
const val ERROR_CODE_OBJECTS_CLIENT = "msg.error.objects.client.exception"
const val ERROR_CODE_OBJECTTYPES_CLIENT = "msg.error.objecttypes.client.exception"
const val ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE = "msg.error.productaanvraagtype.already.in.use"
const val ERROR_CODE_REFERENCE_TABLE_SYSTEM_VALUES_CANNOT_BE_CHANGED =
    "msg.error.system.reference.table.system.values.cannot.be.changed"
const val ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS =
    "msg.error.reference.table.with.same.code.already.exists"
const val ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKAFHANDELPARAMETERS =
    "msg.error.reference.table.is.in.use.by.zaakafhandelparameters"
const val ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED = "msg.error.system.reference.table.cannot.be.deleted"
const val ERROR_CODE_ZRC_CLIENT = "msg.error.zrc.client.exception"
const val ERROR_CODE_ZTC_CLIENT = "msg.error.ztc.client.exception"
const val ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED = "msg.error.smartdocuments.not.configured"
const val ERROR_CODE_SMARTDOCUMENTS_DISABLED = "msg.error.smartdocuments.disabled"
const val ERROR_CODE_BESLUIT_PUBLICATION_DISABLED_TYPE = "msg.error.besluit.publication.disabled"
const val ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE = "msg.error.besluit.publication.date.missing"
const val ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE = "msg.error.besluit.response.date.missing"
const val ERROR_CODE_BESLUIT_RESPONSE_DATE_INVALID_TYPE = "msg.error.besluit.response.date.invalid"
const val ERROR_CODE_CASE_HAS_OPEN_SUBCASES = "msg.error.case.has.open.subs"
const val ERROR_CODE_CASE_HAS_LOCKED_DOCUMENTS = "msg.error.case.has.locked.documents"
