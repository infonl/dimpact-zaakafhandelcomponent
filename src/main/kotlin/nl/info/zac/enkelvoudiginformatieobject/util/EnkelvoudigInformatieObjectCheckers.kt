/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.enkelvoudiginformatieobject.util

import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject

/**
 * Check if EnkelvoudigInformatieObject is signed
 */
fun EnkelvoudigInformatieObject.isSigned() =
    ondertekening != null &&
        ondertekening.datum != null &&
        ondertekening.soort != null
