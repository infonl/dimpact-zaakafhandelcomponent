/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.enkelvoudiginformatieobject.util

import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.SoortEnum

/**
 * Check if EnkelvoudigInformatieObject is signed
 */
fun EnkelvoudigInformatieObject.isSigned() =
    ondertekening != null &&
        ondertekening.datum != null &&
        ondertekening.soort != null &&
        // this extra check is because the API can return an empty ondertekening soort when no signature is present
        // (even if this is not permitted according to the original OpenAPI spec)
        ondertekening.soort != SoortEnum.EMPTY
