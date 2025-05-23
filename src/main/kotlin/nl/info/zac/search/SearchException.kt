/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import nl.info.zac.exception.ErrorCode.ERROR_CODE_SEARCH_SEARCH
import nl.info.zac.exception.ServerErrorException

/**
 * Exception thrown when an error occurs during searches using the Solr search engine.
 */
class SearchException(message: String, cause: Throwable) : ServerErrorException(
    errorCode = ERROR_CODE_SEARCH_SEARCH,
    message = message,
    cause = cause
)
