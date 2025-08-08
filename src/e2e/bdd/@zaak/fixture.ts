import { test as base } from "playwright-bdd";
import { test as authTest } from "../@login/fixture";

export const test = authTest.extend<{
  caseNumber: string;
}>({
  caseNumber: async ({}, use) => {
    await use("");
  },
});
