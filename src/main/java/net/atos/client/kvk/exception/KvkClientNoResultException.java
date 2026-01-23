/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.kvk.exception;

public class KvkClientNoResultException extends RuntimeException {
    public KvkClientNoResultException(String message) {
        super(message);
    }
}
