/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

class FakeEventWithConstant(val count: Int) : LogEvent("Event with a constant") {
    companion object {
        const val FAKE_CONSTANT = "fakeConstant"
    }
}
