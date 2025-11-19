/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { test } from "../fixture";

const bdd = createBdd(test);
if (!bdd) throw new Error("BDD not found"); // This is a dummy for the step file to exist
