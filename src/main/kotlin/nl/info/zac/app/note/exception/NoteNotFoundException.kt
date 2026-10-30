/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note.exception

import jakarta.ws.rs.NotFoundException

class NoteNotFoundException(message: String) : NotFoundException(message)
