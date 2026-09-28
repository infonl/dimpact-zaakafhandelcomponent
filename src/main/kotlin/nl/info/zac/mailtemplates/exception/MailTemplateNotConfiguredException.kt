/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_MAIL_TEMPLATE_NOT_CONFIGURED
import nl.info.zac.exception.ServerErrorException
import nl.info.zac.mailtemplates.model.Mail

class MailTemplateNotConfiguredException(mail: Mail) : ServerErrorException(
    errorCode = ERROR_CODE_MAIL_TEMPLATE_NOT_CONFIGURED,
    message = "No default mail template configured for mail type '${mail.name}'. It must be configured by an administrator."
)
