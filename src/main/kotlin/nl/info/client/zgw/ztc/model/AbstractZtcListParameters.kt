/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam

abstract class AbstractZtcListParameters {
    /**
     * Filter objects depending on their concept status
     */
    private var status: ObjectStatusFilter? = null

    @QueryParam("status")
    fun getStatus() = status?.toValue()

    fun setStatus(status: ObjectStatusFilter) {
        this.status = status
    }
}
