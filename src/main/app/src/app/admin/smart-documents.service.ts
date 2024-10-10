import { Injectable } from "@angular/core";
import { catchError, Observable, map } from "rxjs";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";

export type SmartDocumentsTemplateGroup = {
  id: string;
  name: string;
  groups?: SmartDocumentsTemplateGroup[];
  templates?: SmartDocumentsTemplate[];
};

export type SmartDocumentsTemplate = {
  id: string;
  name: string;
};

export type DocumentsTemplateGroup = {
  id: string;
  name: string;
  groups?: DocumentsTemplateGroup[];
  templates?: DocumentsTemplate[];
};

export type DocumentsTemplate = {
  id: string;
  name: string;
  informatieObjectTypeUUID: string;
};

@Injectable({ providedIn: "root" })
export class SmartDocumentsService {
  constructor(
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  listTemplates(): Observable<SmartDocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET("/rest/zaakafhandelparameters/document-templates")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getTemplatesMapping(
    zaakafhandelUUID: string,
  ): Observable<DocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/document-templates",
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

  getTemplatesMappingFlat(
    zaakafhandelUUID: string,
  ): Observable<DocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/document-templates",
        {
          pathParams: {
            path: {
              zaakafhandelUUID,
            },
          },
        },
      )
      .pipe(
        map((data) => {
          const flattened = data.map(this.flattenGroups).flat();
          console.log("Flattened groups:", flattened);
          return flattened;
        }),
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  storeTemplatesMapping(
    zaakafhandelUUID: string,
    templates: DocumentsTemplateGroup[],
  ) {
    return this.zacHttp
      .POST(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/document-templates",
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

  // Function to flatten groups while ensuring type safety
  flattenGroups = ({
    id,
    name,
    templates = [],
    groups = [],
  }: SmartDocumentsTemplateGroup): DocumentsTemplateGroup[] => {
    const templateInfo: DocumentsTemplate[] = templates.map(({ id, name }) => ({
      id,
      name,
      informatieObjectTypeUUID: "",
    }));

    // Recursively flatten the groups
    return [
      { id, name, templates: templateInfo },
      ...groups.flatMap(this.flattenGroups),
    ];
  };
}
