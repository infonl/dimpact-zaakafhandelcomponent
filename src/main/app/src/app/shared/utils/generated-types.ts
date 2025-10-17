/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { components } from "../../../generated/types/zac-openapi-types";

export type NullableIfOptional<T> = T extends object
  ? {
      [K in keyof T]: undefined extends T[K]
        ? NullableIfOptional<T[K]> | null
        : NullableIfOptional<T[K]>;
    }
  : T;

type NestedSchemaProperty<Type, Key> = Key extends `${infer P}.${infer R}`
  ? P extends keyof Type
    ? NullableIfOptional<NestedSchemaProperty<Type[P], R>>
    : never
  : Key extends keyof Type
    ? NullableIfOptional<Type[Key]>
    : never;

export type GeneratedType<Key extends keyof components["schemas"]> =
  NestedSchemaProperty<components["schemas"], Key>;
