import { test as base } from "playwright-bdd";

export const test = base.extend<{
  caseNumber: string;
}>({
  caseNumber: async ({}, use) => {
    await use("");
  },
});
