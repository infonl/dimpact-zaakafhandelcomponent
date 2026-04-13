/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.shared.model.audit.besluiten

import net.atos.client.zgw.shared.model.ObjectType
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.shared.model.audit.AuditWijziging

class BesluitWijziging : AuditWijziging<Besluit>() {
    override val objectType = ObjectType.BESLUIT
}
