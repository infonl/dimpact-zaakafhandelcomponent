/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent

data object AanvraagPdfNotFound : LogEvent("No PDF found in productaanvraag to link to the zaak")
