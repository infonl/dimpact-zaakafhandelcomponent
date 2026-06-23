/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model.audit.documenten

import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject

fun createEnkelvoudigInformatieobjectWijziging(
    oud: EnkelvoudigInformatieObject? = null,
    nieuw: EnkelvoudigInformatieObject? = null
) = EnkelvoudigInformatieobjectWijziging().apply {
    this.oud = oud
    this.nieuw = nieuw
}
