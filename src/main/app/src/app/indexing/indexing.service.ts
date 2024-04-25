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

  commit(): Observable<void> {
    return this.http
      .post<void>(`${this.basepath}/commit`, null)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
