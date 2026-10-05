/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { screen, waitFor } from "@testing-library/angular";
import { readFileSync } from "node:fs";
import { join } from "node:path";

// Lives outside src/static, because every file in that folder is shipped as-is.
describe.each([
  {
    fileName: "error-403.html",
    message: "U heeft geen toestemming om deze pagina te bekijken.",
  },
  {
    fileName: "error-403-no-read-role.html",
    message:
      "U heeft minstens één basisrol nodig om deze applicatie te kunnen gebruiken.",
  },
])("$fileName", ({ fileName, message }) => {
  const errorPageHtml = readFileSync(
    join(__dirname, "static", fileName),
    "utf-8",
  );
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

  const paragraphTexts = () =>
    screen
      .getAllByRole("paragraph")
      .map((paragraph) => paragraph.textContent?.replace(/\s+/g, " ").trim());

  beforeEach(() => {
    global.fetch = fetchMock;
  });

  afterEach(() => {
    document.body.innerHTML = "";
    fetchMock.mockReset();
  });

  it("requests the server error texts", () => {
    fetchMock.mockResolvedValue({ ok: true, json: async () => [] });

    renderErrorPage();

    expect(fetchMock).toHaveBeenCalledWith(
      "/rest/referentietabellen/server-error-text",
      expect.anything(),
    );
  });

  it("shows each server error text as a paragraph", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ["fakeServerErrorText1", "fakeServerErrorText2"],
    });

    renderErrorPage();

    expect(await screen.findByText("fakeServerErrorText1")).toBeVisible();
    expect(screen.getByText("fakeServerErrorText2")).toBeVisible();
  });

  it("shows a server error text as plain text and not as HTML", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ['<img src="x" alt="fakeImage">'],
    });

    renderErrorPage();

    expect(
      await screen.findByText('<img src="x" alt="fakeImage">'),
    ).toBeVisible();
    expect(screen.queryByRole("img", { name: "fakeImage" })).toBeNull();
  });

  it("shows only its own message when the server error texts are forbidden", async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 403 });

    renderErrorPage();
    await flushPromises();

    expect(paragraphTexts()).toEqual([message]);
  });

  it("shows only its own message when reading the server error texts fails", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));

    renderErrorPage();
    await flushPromises();

    expect(paragraphTexts()).toEqual([message]);
  });

  it("shows the server error texts after its own message", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ["fakeServerErrorText"],
    });

    renderErrorPage();

    await waitFor(() =>
      expect(paragraphTexts()).toEqual([message, "fakeServerErrorText"]),
    );
  });
});
