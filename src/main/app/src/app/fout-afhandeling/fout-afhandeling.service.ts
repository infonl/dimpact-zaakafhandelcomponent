/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { Injectable, inject, isDevMode } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { Observable, of, throwError } from "rxjs";
import { P, match } from "ts-pattern";
import { UtilService } from "../core/service/util.service";
import { HttpParamsError } from "../shared/http/http-client";
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

export type FoutAfhandelingOptions = {
  /**
   * An i18n key for a line that the dialog shows above the error, for a caller
   * that recovered from the failure and tells the user what is shown instead.
   */
  waarschuwing?: string;
  /** An i18n key for the title of the dialog, instead of the general one. */
  titel?: string;
};

/** A `/rest/` request that fails with status 0 means the session has expired. */
export function isLoggedOut(error: HttpErrorResponse) {
  return error.status === 0 && !!error.url?.includes("/rest/");
}

@Injectable({
  providedIn: "root",
})
export class FoutAfhandelingService {
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly utilService = inject(UtilService);
  private readonly translateService = inject(TranslateService);

  foutmelding = "";
  bericht = "";

  /**
   * Once the user logs out, the browser is on its way to `/sign-out`. ZAC ends its session first, so requests of the
   * page that are still running fail as logged out. Reloading the page for them would cancel the logout, and the
   * reloaded page would log the user in again with the Keycloak session that the logout had not ended yet.
   */
  private isLoggingOut = false;

  ignoreLoggedOutErrors() {
    this.isLoggingOut = true;
  }

  foutAfhandelen(
    err: HttpErrorResponse | HttpParamsError | JakartaBeanValidationError,
    options: FoutAfhandelingOptions = {},
  ) {
    return match(err)
      .with(ValidationErrorPattern, (e: JakartaBeanValidationError) =>
        this.validatieErrorAfhandelen(e, options),
      )
      .otherwise((err) => this.httpErrorAfhandelen(err, options));
  }

  public validatieErrorAfhandelen(
    error: JakartaBeanValidationError,
    options: FoutAfhandelingOptions = {},
  ) {
    const flattenedErrorList = Object.values(error.error).flat();
    console.warn("Received a `JakartaBeanValidationError`", error);

    const details = flattenedErrorList
      .map((violation) => this.translateService.instant(violation.message))
      .join("\n");

    return details.length
      ? this.openFoutDetailedDialog(
          this.translateService.instant("msg.error.validation.generic"),
          details,
          undefined,
          options,
        )
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
    { waarschuwing, titel }: FoutAfhandelingOptions = {},
  ) {
    this.dialog.closeAll(); // Make sure that only one error dialog is open at a time
    this.dialog.open(FoutDetailedDialogComponent, {
      data: {
        error,
        details,
        showServerErrorTexts,
        waarschuwing,
        titel,
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

  public httpErrorAfhandelen(
    err: HttpErrorResponse | HttpParamsError,
    options: FoutAfhandelingOptions = {},
  ) {
    if (err instanceof HttpParamsError) {
      return throwError(() => err.message);
    }

    const errorMessage = err.error?.message || err.message;

    const errorDetail = err?.error?.exception;

    // The plain dialog has no room for the waarschuwing, so it goes to the detailed one.
    if (err.status === 400 && !errorDetail && !options.waarschuwing) {
      return this.openFoutDialog(
        this.translateService.instant(
          errorMessage || "dialoog.error.body.technisch",
        ),
      );
    }

    this.foutmelding = err.message;
    if (err.error instanceof ErrorEvent) {
      // client-side error
      this.foutmelding = "dialoog.error.body.fout";
      this.bericht = err.error.message;
      void this.router.navigate(["/fout"]);
      return of();
    }

    if (isLoggedOut(err)) {
      if (this.isLoggingOut) {
        return throwError(() => "User logged out");
      }
      if (!isDevMode()) {
        window.location.reload();
        return throwError(() => "User logged out");
      }
      this.foutmelding = "dialoog.error.body.loggedout";
      this.bericht = "";
      void this.router.navigate(["/fout"]);
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
      options,
    );
  }
}
