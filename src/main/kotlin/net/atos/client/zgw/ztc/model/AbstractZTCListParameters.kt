/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam

/**
 *
 */
abstract class AbstractZTCListParameters {
    /*
     * Filter objects depending on their concept status
     */
    private var status: ObjectStatusFilter? = null

    @QueryParam("status")
    fun getStatus(): String? {
        return if (status != null) status!!.toValue() else null
    }

    fun setStatus(status: ObjectStatusFilter?) {
        this.status = status
    }
}
