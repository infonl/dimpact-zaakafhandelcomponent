/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import nl.info.zac.exception.ErrorCode.ERROR_CODE_SEARCH_INDEXING
import nl.info.zac.exception.ServerErrorException

/**
 * Exception thrown when an error occurs during search indexing in the Solr search engine.
 */
class IndexingException : ServerErrorException {
    constructor(message: String) : super(ERROR_CODE_SEARCH_INDEXING, message)
    constructor(message: String, cause: Throwable) : super(ERROR_CODE_SEARCH_INDEXING, message, cause)
}
