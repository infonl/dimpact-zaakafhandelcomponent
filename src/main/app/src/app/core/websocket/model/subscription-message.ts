/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ScreenEvent } from "./screen-event";
import { SubscriptionType } from "./subscription-type";

/**
 * @deprecated - use the `GeneratedType`
 */
export class SubscriptionMessage {
  subscriptionType: SubscriptionType;
  event: ScreenEvent;

  constructor(subscriptionType: SubscriptionType, event: ScreenEvent) {
    this.subscriptionType = subscriptionType;
    this.event = event;
  }
}
