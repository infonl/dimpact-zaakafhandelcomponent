/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.shared.model.audit.documenten

import net.atos.client.zgw.shared.model.ObjectType
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.shared.model.audit.AuditWijziging

class EnkelvoudigInformatieobjectWijziging : AuditWijziging<EnkelvoudigInformatieObject>() {
    override val objectType = ObjectType.ENKELVOUDIG_INFORMATIEOBJECT
}
