/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn.exception

import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import org.flowable.common.engine.api.FlowableException

class OpenTaskItemNotFoundException(override val message: String) : RuntimeException(message)

class CaseDefinitionNotFoundException(override val message: String) : RuntimeException(message)

/**
 * Custom Flowable exception that wraps ZGW validation errors. It is used to throw ZGW validation errors from inside
 * a Flowable context so that for these exceptions the Flowable command context will log them at INFO log level
 * instead of the default ERROR log level.
 */
class FlowableZgwValidationErrorException(
    override val message: String,
    override val cause: ZgwValidationErrorException
) :
    FlowableException(message, cause)
