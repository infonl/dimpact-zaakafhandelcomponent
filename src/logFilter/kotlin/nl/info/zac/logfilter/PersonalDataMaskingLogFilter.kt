/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

import org.jboss.logmanager.ExtLogRecord
import java.util.logging.Filter
import java.util.logging.LogRecord

/**
 * WildFly logging filter that masks personal data in every log record that reaches the log handler that it is
 * attached to. It never rejects a log record.
 *
 * WildFly loads this class from the `nl.info.zac.logfilter` module, not from the ZAC deployment.
 */
class PersonalDataMaskingLogFilter : Filter {
    override fun isLoggable(record: LogRecord): Boolean {
        if (record is ExtLogRecord) {
            record.formattedMessage?.maskPersonalData()?.let { maskedMessage ->
                // A record that is logged through the java.util.logging API, for example with a message supplier,
                // is a WrappedExtLogRecord, which reads its message and parameters from the original record.
                record.message = maskedMessage
                record.parameters = null
                record.setMessage(maskedMessage, ExtLogRecord.FormatStyle.NO_FORMAT)
            }
        } else {
            record.message = record.message?.maskPersonalData()
        }
        record.thrown = record.thrown?.maskPersonalData()
        return true
    }
}
