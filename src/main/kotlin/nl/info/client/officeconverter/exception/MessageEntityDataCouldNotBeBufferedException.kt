/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.officeconverter.exception

/**
 * Exception thrown when the message entity data could not be buffered.
 *
 * @param message The detail message for the exception.
 */
class MessageEntityDataCouldNotBeBufferedException(message: String) : RuntimeException(message)
