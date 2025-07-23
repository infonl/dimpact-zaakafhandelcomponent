/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.model

/**
 * The company type as specified by the KVK ZOEKEN API.
 */
enum class BedrijfType(val value: String) {
    HOOFDVESTIGING("hoofdvestiging"),
    NEVENVESTIGING("nevenvestiging"),
    RECHTSPERSOON("rechtspersoon");

    override fun toString(): String = value
}
