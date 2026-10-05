/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { screen, waitFor } from "@testing-library/angular";
import { readFileSync } from "node:fs";
import { join } from "node:path";

// Lives outside src/static, because every file in that folder is shipped as-is.
const errorPageHtml = readFileSync(
  join(__dirname, "static", "error-403.html"),
  "utf-8",
);

describe("403 error page", () => {
  const fetchMock = jest.fn();

  function renderErrorPage() {
    const page = new DOMParser().parseFromString(errorPageHtml, "text/html");
    document.body.innerHTML = page.body.innerHTML;
    // A script element has no role, so Testing Library cannot query it.
    // eslint-disable-next-line no-restricted-syntax, testing-library/no-node-access
    for (const script of page.querySelectorAll("script")) {
      new Function(script.textContent ?? "")();
    }
  }

  const flushPromises = () => new Promise((resolve) => setTimeout(resolve));

  const homeButton = () =>
    screen.queryByRole("button", { name: "Ga naar home" });

  beforeEach(() => {
    global.fetch = fetchMock;
  });

  afterEach(() => {
    document.body.innerHTML = "";
    fetchMock.mockReset();
  });

  it("checks whether the home page is accessible", () => {
    fetchMock.mockResolvedValue({ ok: true });

    renderErrorPage();

    expect(fetchMock).toHaveBeenCalledWith(
      "/",
      expect.objectContaining({ method: "HEAD" }),
    );
  });

  it("shows the home button when the home page is accessible", async () => {
    fetchMock.mockResolvedValue({ ok: true });

    renderErrorPage();

    await waitFor(() => expect(homeButton()).toBeVisible());
  });

  it("does not show the home button when the home page is forbidden, so a user without a read role only gets the log-out button", async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 403 });

    renderErrorPage();

    await flushPromises();
    expect(homeButton()).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Uitloggen" })).toBeVisible();
  });

  it("does not show the home button when checking the home page fails", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));

    renderErrorPage();

    await flushPromises();
    expect(homeButton()).not.toBeInTheDocument();
  });
});
