/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { toDialogErrorMessage } from "./dialog-error.util";

describe("toDialogErrorMessage", () => {
  let translateService: TranslateService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [TranslateModule.forRoot()] });
    translateService = TestBed.inject(TranslateService);
  });

  it("returns the backend message from an HttpErrorResponse body", () => {
    const error = new HttpErrorResponse({
      error: { message: "backend zei nee" },
    });

    expect(toDialogErrorMessage(translateService, error)).toBe(
      "backend zei nee",
    );
  });

  it("falls back to the HttpErrorResponse message when the body has none", () => {
    const error = new HttpErrorResponse({ error: {}, status: 500 });

    expect(toDialogErrorMessage(translateService, error)).toBe(error.message);
  });

  it("falls back to the technical-error key for a non-HTTP error", () => {
    expect(toDialogErrorMessage(translateService, new Error("boom"))).toBe(
      "dialoog.error.body.technisch",
    );
  });
});
