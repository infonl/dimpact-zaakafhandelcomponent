/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ScreenEvent } from "./screen-event";

/**
 * @deprecated - use the `GeneratedType`
 */
export interface EventCallback {
  (event: ScreenEvent): void;
}
