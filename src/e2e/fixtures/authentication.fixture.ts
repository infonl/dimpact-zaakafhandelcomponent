import { createBdd, test as base } from "playwright-bdd";

export const test = base.extend<{
  signIn: () => Promise<void>;
  userToLogin: { username: string; password: string };
}>({
  signIn: async ({ page, userToLogin }, use) => {
    await use(async () => {
      const signInRequest = page.waitForRequest(/login-actions\/authenticate/);

      await page
        .getByRole("textbox", { name: "Username or email" })
        .fill(userToLogin.username);
      await page
        .getByRole("textbox", { name: "Password" })
        .fill(userToLogin.password);
      await page.getByRole("button", { name: "Sign In" }).click();
      await signInRequest;
      await page.waitForLoadState("domcontentloaded");
    });
  },
  userToLogin: async ({}, use) => {
    await use({ username: "", password: "" });
  },
});
