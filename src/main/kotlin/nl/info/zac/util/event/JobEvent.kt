/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.event

import nl.info.zac.event.AbstractEvent
import nl.info.zac.event.Opcode

class JobEvent : AbstractEvent<String, JobId> {
    override val objectType = "cron job"

    /**
     * Constructor for the sake of JAXB
     */
    constructor() : super()

    /**
     * @param jobId indicates the job that must be started
     */
    constructor(jobId: JobId) : super(Opcode.CREATED, jobId)
}
