/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

/**
 * Zaak UUID class as used by [nl.info.client.zgw.ztc.ZtcClient].
 * The variable requires a setter function for the Eclipse MicroProfile REST Client library.
 *
 * @param uuid Unieke resource identifier (UUID4)
 */
@NoArgConstructor
@AllOpen
data class ZaakUuid(var uuid: UUID)
