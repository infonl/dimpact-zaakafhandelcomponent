/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_OPA_RULE_NOT_CONFIGURED
import nl.info.zac.exception.ServerErrorException

class OpaRuleNotConfiguredException(rulePath: String) : ServerErrorException(
    errorCode = ERROR_CODE_OPA_RULE_NOT_CONFIGURED,
    message = "OPA returned no result for rule path '$rulePath'. The rule may be missing from the policy bundle."
)
