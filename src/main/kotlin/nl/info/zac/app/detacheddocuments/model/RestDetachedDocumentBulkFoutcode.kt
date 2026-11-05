/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

enum class RestDetachedDocumentBulkFoutcode {
    NIET_GEVONDEN,
    ONTBREEKT_IN_OPEN_ZAAK,
    VERGRENDELD,
    REEDS_GEKOPPELD,
    GEEN_RECHT,
    OPEN_ZAAK_FOUT,
    OPEN_ZAAK_NIET_BESCHIKBAAR
}
