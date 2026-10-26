/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

type ScreenEventId = GeneratedType<"ScreenEventId">;

export class ScreenEvent {
  private _timestamp?: number;
  opcode: GeneratedType<"Opcode">;
  objectType: GeneratedType<"ScreenEventType">;
  objectId: ScreenEventId;
  readonly actorUsername?: string | null;
  private _key: string;

  constructor(
    opcode: GeneratedType<"Opcode">,
    objectType: GeneratedType<"ScreenEventType">,
    objectId: ScreenEventId,
    timestamp?: number,
    actorUsername?: string | null,
  ) {
    this._timestamp = timestamp;
    this.actorUsername = actorUsername;
    this.opcode = opcode;
    this.objectType = objectType;
    this.objectId = objectId;
    this._key = this.makeKey(this.opcode, this.objectType, this.objectId);
  }

  get timestamp() {
    return this._timestamp;
  }

  get key(): string {
    return this._key;
  }

  get keyAnyOpcode(): string {
    return this.makeKey("ANY", this.objectType, this.objectId);
  }

  get keyAnyObjectType(): string {
    return this.makeKey(this.opcode, "ANY", this.objectId);
  }

  get keyAnyOpcodeAndObjectType(): string {
    return this.makeKey("ANY", "ANY", this.objectId);
  }

  private makeKey(
    opcode: GeneratedType<"Opcode">,
    objectType: GeneratedType<"ScreenEventType">,
    objectId: ScreenEventId,
  ): string {
    return opcode + ";" + objectType + ";" + objectId.resource;
  }
}
