/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken

import nl.info.zac.exception.ErrorCode.ERROR_CODE_SEARCH_INDEXING
import nl.info.zac.exception.ZacRuntimeException

/**
 * Exception thrown when an error occurs during search indexing in the Solr search engine.
 */
class IndexingException : ZacRuntimeException {
    constructor(message: String) : super(ERROR_CODE_SEARCH_INDEXING, message)
    constructor(cause: Throwable) : super(ERROR_CODE_SEARCH_INDEXING, cause)
}
