/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.shared.model.audit

import jakarta.json.bind.annotation.JsonbTypeDeserializer
import net.atos.client.zgw.shared.model.ObjectType
import nl.info.client.zgw.shared.util.AuditWijzigingJsonbDeserializer

@JsonbTypeDeserializer(AuditWijzigingJsonbDeserializer::class)
abstract class AuditWijziging<T> {
    var oud: T? = null
    var nieuw: T? = null

    abstract val objectType: ObjectType
}
