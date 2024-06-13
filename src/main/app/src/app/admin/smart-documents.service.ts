import { Injectable } from "@angular/core";
import { catchError, Observable } from "rxjs";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";

export type DocumentsTemplateGroup = {
  id: string;
  name: string;
  groups?: DocumentsTemplateGroup[];
  templates?: DocumentsTemplate[];
};

export type DocumentsTemplate = {
  id: string;
  name: string;
};

@Injectable({ providedIn: "root" })
export class SmartDocumentsService {
  constructor(
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  listTemplates(): Observable<DocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET("/rest/smartdocuments/templates")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getTemplatesMapping(
    zaakafhandelUUID: string,
  ): Observable<DocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET(
        "/rest/smartdocuments/templates/zaakafhandelParamaters/{zaakafhandelUUID}",
        {
          pathParams: {
            path: {
              zaakafhandelUUID,
            },
          },
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  storeTemplatesMapping(
    zaakafhandelUUID: string,
    templates: DocumentsTemplateGroup[],
  ) {
    return this.zacHttp
      .POST(
        "/rest/smartdocuments/templates/zaakafhandelParamaters/{zaakafhandelUUID}",
        templates,
        {
          pathParams: {
            path: {
              zaakafhandelUUID,
            },
          },
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
