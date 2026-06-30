/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { firstValueFrom } from "rxjs";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { UtilService } from "../core/service/util.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

describe("FoutAfhandelingService", () => {
  let service: FoutAfhandelingService;
  const translatedErrorMessage = "fakeTranslatedErrorMessage";

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialog, useValue: { open() {}, closeAll() {} } },
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
});
