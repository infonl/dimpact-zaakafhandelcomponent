/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_GROUP_NOT_FOUND_IN_KEYCLOAK
import nl.info.zac.exception.InputValidationFailedException

class GroupNotFoundException : InputValidationFailedException(ERROR_CODE_GROUP_NOT_FOUND_IN_KEYCLOAK)
