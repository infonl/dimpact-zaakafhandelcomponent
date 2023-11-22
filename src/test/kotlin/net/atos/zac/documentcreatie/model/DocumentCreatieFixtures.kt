/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreatie.model

import java.net.URI

fun createDocumentCreatieResponse(
    redirectURI: URI = URI.create("http://example.com/dummyRedirectURI")
) = DocumentCreatieResponse(
    redirectURI
)
