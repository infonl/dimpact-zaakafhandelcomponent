/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { firstValueFrom } from "rxjs";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { UtilService } from "../core/service/util.service";
import { FoutDetailedDialogComponent } from "./dialog/fout-detailed-dialog.component";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

describe("FoutAfhandelingService", () => {
  let service: FoutAfhandelingService;
  const translatedErrorMessage = "fakeTranslatedErrorMessage";
  const dialog = { open: jest.fn(), closeAll: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialog, useValue: dialog },
        { provide: UtilService, useValue: { openSnackbarError() {} } },
        {
          provide: TranslateService,
          useValue: {
            instant() {
              return translatedErrorMessage;
            },
          },
        },
        {
          provide: ReferentieTabelService,
          useValue: { listServerErrorTexts() {} },
        },
      ],
      imports: [],
    });

    service = TestBed.inject(FoutAfhandelingService);
  });

  it("should return an observable error message when openFoutDialog is called", async () => {
    const errorText = "fakeErrorMessage";
    const error$ = service.openFoutDialog(errorText);
    const errorMessage = await firstValueFrom(error$).catch((r) => r);
    expect(errorMessage).toEqual(errorText);
  });

  it("should return an observable error message when httpErrorAfhandelen is called with a server error", async () => {
    const exceptionMessage = "fakeException";
    const errorResponse = new HttpErrorResponse({
      error: {
        message: "fakeServerErrorMessage",
        exception: exceptionMessage,
      },
      status: 500,
    });
    const error$ = service.httpErrorAfhandelen(errorResponse);
    const errorMessage = await firstValueFrom(error$).catch((r) => r);
    expect(errorMessage).toEqual(
      `${translatedErrorMessage}: ${exceptionMessage}`,
    );
  });

  it("should return an observable error message when httpErrorAfhandelen is called with a 400 error", async () => {
    const message = "fakeBadRequestMessage";
    const errorResponse = new HttpErrorResponse({
      error: {
        message: message,
      },
      status: 400,
    });

    const error$ = service.httpErrorAfhandelen(errorResponse);
    const errorMessage = await firstValueFrom(error$).catch((r) => r);

    expect(errorMessage).toEqual(`${translatedErrorMessage}`);
  });

  it("should return an observable error message with details when httpErrorAfhandelen is called with a 400 error with exception details", async () => {
    const message = "fakeBadRequestMessage";
    const exceptionDetail = "fakeBadRequestExceptionDetail";
    const errorResponse = new HttpErrorResponse({
      error: {
        message: message,
        exception: exceptionDetail,
      },
      status: 400,
    });

    const error$ = service.httpErrorAfhandelen(errorResponse);
    const errorMessage = await firstValueFrom(error$).catch((r) => r);

    expect(errorMessage).toEqual(
      `${translatedErrorMessage}: ${exceptionDetail}`,
    );
  });

  it("should translate the violation messages when a Jakarta bean validation error is handled", async () => {
    const validationError = {
      statusText: "Bad Request",
      error: {
        classViolations: [],
        parameterViolations: [
          {
            constraintType: "PARAMETER",
            message: "msg.error.document.upload.invalid",
            path: "createEnkelvoudigInformatieobjectAndUploadFile.arg3",
            value: "RestEnkelvoudigInformatieobject(...)",
          },
        ],
        propertyViolations: [],
        returnValueViolations: [],
      },
    } as Parameters<FoutAfhandelingService["foutAfhandelen"]>[0];

    const error$ = service.foutAfhandelen(validationError);
    const errorMessage = await firstValueFrom(error$).catch((r) => r);

    // The violation message and the generic title both pass through TranslateService,
    // and openFoutDetailedDialog rejects with `${title}: ${details}`.
    expect(errorMessage).toEqual(
      `${translatedErrorMessage}: ${translatedErrorMessage}`,
    );
  });
  describe("A request that fails because ZAC ended the session", () => {
    const loggedOutError = new HttpErrorResponse({
      status: 0,
      url: "https://zac.example.com/rest/identity/loggedInUser",
    });

    it("shows the logged-out error page", async () => {
      const navigate = jest
        .spyOn(TestBed.inject(Router), "navigate")
        .mockResolvedValue(true);

      await firstValueFrom(service.foutAfhandelen(loggedOutError), {
        defaultValue: undefined,
      });

      expect(navigate).toHaveBeenCalledWith(["/fout"]);
      expect(service.foutmelding).toBe("dialoog.error.body.loggedout");
    });

    it("leaves the page alone once the user logs out, so that a reload does not cancel the logout", async () => {
      const navigate = jest.spyOn(TestBed.inject(Router), "navigate");
      service.ignoreLoggedOutErrors();

      const errorMessage = await firstValueFrom(
        service.foutAfhandelen(loggedOutError),
      ).catch((reason) => reason);

      expect(errorMessage).toBe("User logged out");
      expect(navigate).not.toHaveBeenCalled();
    });
  });

  describe("given a waarschuwing", () => {
    const waarschuwing = "fakeWaarschuwing";

    it("shows it in the detailed dialog, above the error of the server", () => {
      service.foutAfhandelen(
        new HttpErrorResponse({
          error: { message: "fakeMessage", exception: "fakeException" },
          status: 500,
        }),
        { waarschuwing },
      );

      expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
        data: expect.objectContaining({
          waarschuwing,
          details: "fakeException",
        }),
      });
    });

    it("shows a bad request without details in the detailed dialog as well, since the plain dialog has no room for it", () => {
      service.foutAfhandelen(
        new HttpErrorResponse({
          error: { message: "fakeMessage" },
          status: 400,
        }),
        { waarschuwing },
      );

      expect(dialog.open).toHaveBeenCalledTimes(1);
      expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
        data: expect.objectContaining({ waarschuwing }),
      });
    });

    it("shows the title it is given instead of the general one", () => {
      service.foutAfhandelen(
        new HttpErrorResponse({
          error: { message: "fakeMessage", exception: "fakeException" },
          status: 500,
        }),
        { waarschuwing, titel: "fakeTitel" },
      );

      expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
        data: expect.objectContaining({ waarschuwing, titel: "fakeTitel" }),
      });
    });
  });
});
