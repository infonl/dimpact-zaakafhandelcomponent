import { test as base } from "../@login/fixture";

export const test = base.extend<{
  caseNumber: { value: string };
}>({
  caseNumber: async ({}, use) => {
    await use({ value: "" });
  },
});
