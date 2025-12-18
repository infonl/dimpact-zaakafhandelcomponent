/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { groups } from "./groups";

const Oscar = {
  username: "E2etest User2",
  group: groups.TestGroupB.name,
};

const Bob = {
  username: "E2etest User1",
  group: groups.TestGroupA.name,
};

export const users = {
  Bob,
  Oscar,
};
