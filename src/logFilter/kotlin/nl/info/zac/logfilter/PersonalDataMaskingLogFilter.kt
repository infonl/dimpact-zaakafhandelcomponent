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
 * attached to.
 *
 * WildFly loads this class from the `nl.info.zac.logfilter` module, not from the ZAC deployment.
 */
class PersonalDataMaskingLogFilter : Filter {
    @Suppress("TooGenericExceptionCaught")
    override fun isLoggable(record: LogRecord): Boolean {
        try {
            record.maskPersonalData()
        } catch (runtimeException: RuntimeException) {
            // WildFly drops a log record without any report when its filter throws an exception
            record.withholdContent(runtimeException)
        }
        return true
    }

    private fun LogRecord.maskPersonalData() {
        if (this is ExtLogRecord) {
            formattedMessage?.maskPersonalData()?.let { maskedMessage ->
                // A record that is logged through the java.util.logging API, for example with a message supplier,
                // is a WrappedExtLogRecord, which reads its message and parameters from the original record.
                message = maskedMessage
                parameters = null
                setMessage(maskedMessage, ExtLogRecord.FormatStyle.NO_FORMAT)
            }
        } else {
            message = message?.maskPersonalData()
        }
        thrown = thrown?.maskPersonalData()
    }

    private fun LogRecord.withholdContent(runtimeException: RuntimeException) {
        val withheldMessage = "The content of this log record is not logged, because masking its personal data failed " +
            "with ${runtimeException.javaClass.name}"
        message = withheldMessage
        parameters = null
        if (this is ExtLogRecord) setMessage(withheldMessage, ExtLogRecord.FormatStyle.NO_FORMAT)
        thrown = null
    }
}
