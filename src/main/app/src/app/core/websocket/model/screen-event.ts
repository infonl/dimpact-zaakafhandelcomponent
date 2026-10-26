/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";
import { ObjectType } from "./object-type";
import { Opcode } from "./opcode";

type ScreenEventId = GeneratedType<"ScreenEventId">;

export class ScreenEvent {
  private _timestamp?: number;
  opcode: Opcode;
  objectType: ObjectType;
  objectId: ScreenEventId;
  readonly actorUsername?: string | null;
  private _key: string;

  constructor(
    opcode: Opcode,
    objectType: ObjectType,
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
    return this.makeKey(Opcode.ANY, this.objectType, this.objectId);
  }

  get keyAnyObjectType(): string {
    return this.makeKey(this.opcode, ObjectType.ANY, this.objectId);
  }

  get keyAnyOpcodeAndObjectType(): string {
    return this.makeKey(Opcode.ANY, ObjectType.ANY, this.objectId);
  }

  private makeKey(
    opcode: Opcode,
    objectType: ObjectType,
    objectId: ScreenEventId,
  ): string {
    return opcode + ";" + objectType + ";" + objectId.resource;
  }
}
