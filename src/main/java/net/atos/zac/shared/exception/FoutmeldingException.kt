/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.exception

/**
 * Deze exceptie moet leiden tot een foutboodschap naar de gebruiker
 * ToDo: #1258
 */
class FoutmeldingException(message: String?) : RuntimeException(message)
