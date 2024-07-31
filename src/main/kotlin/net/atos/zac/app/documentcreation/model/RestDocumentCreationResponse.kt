/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.documentcreation.model

import nl.lifely.zac.util.NoArgConstructor
import java.net.URI

@NoArgConstructor
data class RestDocumentCreationResponse(val redirectURL: URI? = null, val message: String? = null)
