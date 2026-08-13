/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import java.util.UUID

val ZaakInformatieObject.zaakUUID: UUID
    get() = zaak.extractUuid()
