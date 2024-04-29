import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, catchError } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";

@Injectable({
  providedIn: "root",
})
export class IndexingService {
  private basepath = "/rest/indexeren";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  index(count = 100): Observable<string> {
    return this.http
      .get(
        `${this.basepath}/${count}`,
        {responseType: 'text'}
      )
  }

  commitPendingChangesToSearchIndex(): Observable<void> {
    return this.http
      .post<void>(
        `${this.basepath}/commit-pending-changes-to-search-index`,
        null,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
