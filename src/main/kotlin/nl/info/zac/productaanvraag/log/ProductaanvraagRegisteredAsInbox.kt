/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent

class ProductaanvraagRegisteredAsInbox(
    val productaanvraagType: String?
) : LogEvent("No zaaktype configured for the productaanvraag-Dimpact type; registering it as inbox productaanvraag")
