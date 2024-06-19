/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { Injectable, isDevMode } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { Observable, throwError } from "rxjs";
import { P, match } from "ts-pattern";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { UtilService } from "../core/service/util.service";
import { FoutDetailedDialogComponent } from "./dialog/fout-detailed-dialog.component";
import { FoutDialogComponent } from "./dialog/fout-dialog.component";

const ViolationPattern = {
  constraintType: P.string,
  message: P.string,
  path: P.string,
  value: P.string,
};

const ValidationErrorPattern = {
  statusText: "Bad Request",
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
  foutmelding: string;
  bericht: string;
  serverErrorTexts: Observable<string[]>;

  constructor(
    private router: Router,
    private dialog: MatDialog,
    private utilService: UtilService,
    private translate: TranslateService,
    private referentieTabelService: ReferentieTabelService,
  ) {}

  public foutAfhandelen(
    err: HttpErrorResponse | JakartaBeanValidationError,
  ): Observable<never> {
    return match(err)
      .with(ValidationErrorPattern, (e: JakartaBeanValidationError) =>
        this.validatieErrorAfhandelen(e),
      )
      .otherwise((err: HttpErrorResponse) => this.httpErrorAfhandelen(err));
  }

  public validatieErrorAfhandelen(err: JakartaBeanValidationError) {
    const flattenedErrorList = [
      err.error.propertyViolations,
      err.error.returnValueViolations,
      err.error.classViolations,
      err.error.parameterViolations,
    ].flat();
    const firstError = flattenedErrorList.find(Boolean);
    return firstError
      ? this.openFoutDetailedDialog(
          "Validatie fout",
          JSON.stringify(firstError),
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
    errorHelpTexts?: Observable<string[]>,
  ): Observable<never> {
    this.dialog.open(FoutDetailedDialogComponent, {
      data: {
        error,
        details,
        errorHelpTexts,
      },
    });

    return throwError(() => "Fout!");
  }

  public log(melding): (error: HttpErrorResponse) => Observable<any> {
    return (error: any): Observable<never> => {
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

  public httpErrorAfhandelen(err: HttpErrorResponse): Observable<never> {
    this.foutmelding = err.message;
    if (err.error instanceof ErrorEvent) {
      // client-side error
      this.foutmelding = `Er is een fout opgetreden`;
      this.bericht = err.error.message;
      this.router.navigate(["/fout-pagina"]);
    } else if (err.status === 0 && err.url.startsWith("/rest/")) {
      // status 0 means that the user is no longer logged in
      if (!isDevMode()) {
        window.location.reload();
        return;
      }
      this.foutmelding = "Helaas! Je bent uitgelogd.";
      this.bericht = "";
      this.router.navigate(["/fout-pagina"]);
    } else {
      let errorDetail: string;
      if (err.error) {
        errorDetail = err.error.exception;
      } else {
        errorDetail = err.message;
      }
      let serverErrorText: Observable<string[]> | undefined;
      // only show server error texts in case of a server error (500 family of errors)
      if (err.status >= 500) {
        serverErrorText = this.referentieTabelService.listServerErrorTexts();
      }
      // show error in context and do not redirect to error page
      return this.openFoutDetailedDialog(
        this.translate.instant(err.error.message),
        errorDetail,
        serverErrorText,
      );
    }
    return throwError(() => `${this.foutmelding}: ${this.bericht}`);
  }
}
