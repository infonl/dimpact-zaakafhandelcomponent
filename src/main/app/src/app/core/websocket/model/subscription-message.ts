/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";
import { ScreenEvent } from "./screen-event";

/**
 * `WebSocketSubscriptionMessageDecoder` only reads these fields of the event.
 */
type SubscribedEvent = Pick<
  GeneratedType<"ScreenEvent">,
  "opcode" | "objectType" | "objectId"
>;

export type SubscriptionMessage = Omit<
  GeneratedType<"SubscriptionMessage">,
  "event"
> & { event: SubscribedEvent };

export function subscriptionMessage(
  subscriptionType: Exclude<GeneratedType<"SubscriptionType">, "DELETE_ALL">,
  event: ScreenEvent,
): SubscriptionMessage {
  return {
    subscriptionType,
    event: {
      opcode: event.opcode,
      objectType: event.objectType,
      objectId: event.objectId,
    },
  };
}
