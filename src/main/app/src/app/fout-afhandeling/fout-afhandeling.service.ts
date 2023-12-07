/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, isDevMode } from "@angular/core";
import {Observable, throwError} from "rxjs";
import { Router } from "@angular/router";
import { HttpErrorResponse } from "@angular/common/http";
import { MatDialog } from "@angular/material/dialog";
import { FoutDialogComponent } from "./dialog/fout-dialog.component";
import { UtilService } from "../core/service/util.service";
import { match, P } from 'ts-pattern';
import {FoutDetailedDialogComponent} from "./dialog/fout-detailed-dialog.component";

const ViolationPattern = {
  "constraintType": P.string,
  "message": P.string,
  "path": P.string,
  "value": P.string
}

const ValidationErrorPattern = {
  statusText: 'Bad Request',
  error: {
    classViolations: P.array(ViolationPattern),
    parameterViolations: P.array(ViolationPattern),
    propertyViolations: P.array(ViolationPattern),
    returnValueViolations: P.array(ViolationPattern)
  }
}

type JakartaBeanValidationError = P.infer<typeof ValidationErrorPattern>

@Injectable({
  providedIn: "root",
})
export class FoutAfhandelingService {
  foutmelding: string;
  bericht: string;
  stack: string;
  exception: string;

  constructor(
    private router: Router,
    private dialog: MatDialog,
    private utilService: UtilService,
  ) {}

  public foutAfhandelen(err: HttpErrorResponse | JakartaBeanValidationError): Observable<any> {

   return match(err)
       .with(ValidationErrorPattern, (e: JakartaBeanValidationError) => this.validatieErrorAfhandelen(e))
       .otherwise((err: HttpErrorResponse) => this.httpErrorAfhandelen(err))
  }

  public validatieErrorAfhandelen(err: JakartaBeanValidationError) {
      const flattenedErrorList = [
        err.error.propertyViolations,
        err.error.returnValueViolations,
        err.error.classViolations,
        err.error.parameterViolations,
      ].flat()
      const firstError = flattenedErrorList.find(Boolean)
      return firstError ? this.openFoutDetailedDialog("Validatie fout", JSON.stringify(firstError)) : throwError(() => new Error('No violations found'))
  }

  public httpErrorAfhandelen(err: HttpErrorResponse) {
    if (err.status === 400) {
      return this.openFoutDialog(err.error);
    } else {
      return this.redirect(err);
    }
  }

  public openFoutDialog(error: string): Observable<any> {
    this.dialog.open(FoutDialogComponent, {
      data: error,
    });

    return throwError(() => "Fout!");
  }

  public openFoutDetailedDialog(error: string, details: string): Observable<any> {
    this.dialog.open(FoutDetailedDialogComponent, {
      data: {
        error,
        details,
      },
    });

    return throwError(() => "Fout!");
  }

  private redirect(err: HttpErrorResponse): Observable<never> {
    this.foutmelding = err.message;
    if (err.error instanceof ErrorEvent) {
      // Client-side
      this.foutmelding = `Er is een fout opgetreden`;
      this.bericht = err.error.message;
      this.exception = "";
      this.stack = "";
    } else if (err.status === 0 && err.url.startsWith("/rest/")) {
      // status 0, niet meer ingelogd
      if (!isDevMode()) {
        window.location.reload();
        return;
      }
      this.foutmelding = "Helaas! Je bent uitgelogd.";
      this.bericht = "";
    } else {
      this.foutmelding = `De server heeft code ${err.status} geretourneerd`;
      if (err.error) {
        this.stack = err.error.stackTrace;
        this.bericht = err.error.message;
        this.exception = err.error.exception;
      } else {
        this.exception = "";
        this.stack = "";
        this.bericht = err.message;
      }
    }
    if (isDevMode()) {
      this.utilService.openSnackbarError(this.foutmelding);
      if (this.stack) {
        console.error(this.stack);
      }
    } else {
      this.router.navigate(["/fout-pagina"]);
    }
    return throwError(() => `${this.foutmelding}: ${this.bericht}`);
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
}
