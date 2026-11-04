/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum

enum class RestArchiefnominatie {
    BLIJVEND_BEWAREN,
    VERNIETIGEN
}

fun RestArchiefnominatie.toArchiefnominatieEnum() = ArchiefnominatieEnum.valueOf(name)
