/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

/**
 * ZAC REST API enum for vertrouwelijkheidaanduiding.
 *
 * This is a separate enum from the ZGW-generated [VertrouwelijkheidaanduidingEnum] types because
 * those are serialized as lowercase by JSON-B (for ZGW API compatibility), while the ZAC REST API
 * needs to consistently use UPPERCASE values matching the OpenAPI spec.
 */
enum class RestVertrouwelijkheidaanduiding {
    OPENBAAR,
    BEPERKT_OPENBAAR,
    INTERN,
    ZAAKVERTROUWELIJK,
    VERTROUWELIJK,
    CONFIDENTIEEL,
    GEHEIM,
    ZEER_GEHEIM
}

fun nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum.toRestVertrouwelijkheidaanduiding() =
    RestVertrouwelijkheidaanduiding.entries.firstOrNull { it.name == name }

fun nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum.toRestVertrouwelijkheidaanduiding() =
    RestVertrouwelijkheidaanduiding.valueOf(name)

fun nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum.toRestVertrouwelijkheidaanduiding() =
    RestVertrouwelijkheidaanduiding.valueOf(name)

fun RestVertrouwelijkheidaanduiding.toDrcVertrouwelijkheidaanduidingEnum() =
    nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum.valueOf(name)

fun RestVertrouwelijkheidaanduiding.toZrcVertrouwelijkheidaanduidingEnum() =
    nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum.valueOf(name)
