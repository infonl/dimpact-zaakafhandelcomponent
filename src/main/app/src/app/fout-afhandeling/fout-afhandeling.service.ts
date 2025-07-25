/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { Injectable, isDevMode } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { Observable, of, throwError } from "rxjs";
import { P, match } from "ts-pattern";
import { UtilService } from "../core/service/util.service";
import { HttpParamsError } from "../shared/http/zac-http-client";
import { FoutDetailedDialogComponent } from "./dialog/fout-detailed-dialog.component";
import { FoutDialogComponent } from "./dialog/fout-dialog.component";

const ViolationPattern = {
  constraintType: P.string,
  message: P.string,
  path: P.string,
  value: P.string,
};

const ValidationErrorPattern = {
  statusText: "Bad Request" as const,
  error: {
    classViolations: P.array(ViolationPattern),
    parameterViolations: P.array(ViolationPattern),
    propertyViolations: P.array(ViolationPattern),
    returnValueViolations: P.array(ViolationPattern),
  },
};

type JakartaBeanValidationError = P.infer<typeof ValidationErrorPattern>;

@Injectable({
  providedIn: "root",
})
export class FoutAfhandelingService {
  foutmelding = "";
  bericht = "";

  constructor(
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly utilService: UtilService,
    private readonly translateService: TranslateService,
  ) {}

  foutAfhandelen(
    err: HttpErrorResponse | HttpParamsError | JakartaBeanValidationError,
  ) {
    return match(err)
      .with(ValidationErrorPattern, (e: JakartaBeanValidationError) =>
        this.validatieErrorAfhandelen(e),
      )
      .otherwise((err) => this.httpErrorAfhandelen(err));
  }

  public validatieErrorAfhandelen(error: JakartaBeanValidationError) {
    const flattenedErrorList = Object.values(error.error).flat();
    console.warn("Received a `JakartaBeanValidationError`", error);

    const details = flattenedErrorList.reduce((acc, violation) => {
      return `${acc}- ${violation.constraintType}: ${violation.message} (path: ${violation.path}, value: "${violation.value}")\n`;
    }, "");

    return details.length
      ? this.openFoutDetailedDialog("Validatie fout", details)
      : throwError(() => new Error("No violations found"));
  }

  public openFoutDialog(errorText: string): Observable<never> {
    this.dialog.open(FoutDialogComponent, {
      data: errorText,
    });

    return throwError(() => errorText);
  }

  public openFoutDetailedDialog(
    error: string,
    details: string,
    showServerErrorTexts?: boolean,
  ) {
    this.dialog.open(FoutDetailedDialogComponent, {
      data: {
        error,
        details,
        showServerErrorTexts,
      },
    });

    return throwError(() => `${error}: ${details}`);
  }

  public log(melding: string) {
    return (error: unknown) => {
      console.error(error); // log to console instead
      this.utilService.openSnackbarError(melding);
      return throwError(error);
    };
  }

  public getFout(e: HttpErrorResponse) {
    if (e.error instanceof ErrorEvent) {
      return `Er is een fout opgetreden. (${e.error.message})`;
    } else {
      if (e.error) {
        return `De server heeft code ${e.status} geretourneerd. (${e.error.exception})`;
      } else {
        return e.message;
      }
    }
  }

  public httpErrorAfhandelen(err: HttpErrorResponse | HttpParamsError) {
    if (err instanceof HttpParamsError) {
      return throwError(() => err.message);
    }

    const errorMessage = err.error?.message || err.message;

    const errorDetail = err?.error?.exception;

    if (err.status === 400 && !errorDetail) {
      return this.openFoutDialog(
        this.translateService.instant(
          errorMessage || "dialoog.error.body.technisch",
        ),
      );
    }

    this.foutmelding = err.message;
    if (err.error instanceof ErrorEvent) {
      // client-side error
      this.foutmelding = this.translateService.instant(
        "dialoog.error.body.fout",
      );
      this.bericht = err.error.message;
      void this.router.navigate(["/fout-pagina"]);
      return of();
    }

    if (err.status === 0 && err.url?.startsWith("/rest/")) {
      // status 0 means that the user is no longer logged in
      if (!isDevMode()) {
        window.location.reload();
        return throwError(() => "User logged out");
      }
      this.foutmelding = this.translateService.instant(
        "dialoog.error.body.loggedout",
      );
      this.bericht = "";
      void this.router.navigate(["/fout-pagina"]);
      return of();
    }

    // only show server error texts in case of a server error (500 family of errors)
    // or in case of a 403 Forbidden error
    const showServerErrorTexts = err.status >= 500 || err.status === 403;

    // show error in context and do not redirect to error-page
    return this.openFoutDetailedDialog(
      this.translateService.instant(
        errorMessage || "dialoog.error.body.technisch",
      ),
      errorDetail ?? this.translateService.instant("dialoog.error.body.fout"),
      showServerErrorTexts,
    );
  }
}
