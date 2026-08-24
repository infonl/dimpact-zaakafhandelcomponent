/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import java.io.Serializable
import java.net.URI

/**
 * An error that occurred in one of the ZGW APIs.
 */
open class ZgwError @JsonbCreator constructor(
    // URI-referentie naar het type fout, bedoeld voor developers
    @param:JsonbProperty("type") val type: URI?,
    // Systeemcode die het type fout aangeeft
    @param:JsonbProperty("code") val code: String?,
    // Generieke titel voor het type fout
    @param:JsonbProperty("title") val title: String?,
    // De HTTP status code
    @param:JsonbProperty("status") val status: Int,
    // Extra informatie bij de fout, indien beschikbaar
    @param:JsonbProperty("detail") val detail: String?,
    // URI met referentie naar dit specifiek voorkomen van de fout.
    // Deze kan gebruikt worden in combinatie met server logs, bijvoorbeeld.
    @param:JsonbProperty("instance") val instance: URI?
) : Serializable {
    override fun toString() = "($status) Title: $title, Detail: $detail"

    companion object {
        private const val serialVersionUID: Long = 67354364654464L
    }
}
