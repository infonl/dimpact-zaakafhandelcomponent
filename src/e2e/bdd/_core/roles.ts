/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import path from "node:path";
import { ROLES, type Role } from "../../config/targets/target";

const SESSIONS_DIRECTORY = path.resolve(__dirname, "../../.auth");

export function isRole(name: string): name is Role {
  return ROLES.some((role) => role === name);
}

export function roleOfTags(tags: string[]): Role | undefined {
  const roles = ROLES.filter((role) => tags.includes(`@${role}`));
  if (roles.length > 1) {
    throw new Error(
      `A scenario runs as one role, but it has the role tags ${roles.map((role) => `@${role}`).join(" and ")}.`,
    );
  }
  return roles[0];
}

export function sessionFileOfRole(role: Role) {
  return path.join(SESSIONS_DIRECTORY, `${role}.json`);
}
