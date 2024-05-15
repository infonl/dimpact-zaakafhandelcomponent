/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpClient,
  HttpEvent,
  HttpEventType,
  HttpHeaders,
} from "@angular/common/http";
import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subject, Subscription, takeUntil } from "rxjs";
import { FoutAfhandelingService } from "../../../../fout-afhandeling/fout-afhandeling.service";
import { FormComponent } from "../../model/form-component";
import { FileFormField } from "./file-form-field";
import { UploadStatus } from "./upload-status.enum";

@Component({
  templateUrl: "./file.component.html",
  styleUrls: ["./file.component.less"],
})
export class FileComponent extends FormComponent implements OnInit, OnDestroy {
  @ViewChild("fileInput") fileInput: ElementRef;

  data: FileFormField;
  progress = 0;
  subscription: Subscription;
  status: string = UploadStatus.SELECTEER_BESTAND;
  destroy$ = new Subject<void>();

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
    private changeDetector: ChangeDetectorRef,
    public translate: TranslateService,
  ) {
    super();
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnInit(): void {
    this.data.reset$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.reset();
    });
  }

  uploadFile(file: File) {
    this.data.uploadError = null;
    if (file) {
      if (!this.data.isBestandstypeToegestaan(file)) {
        this.data.uploadError = `Het bestandstype is niet toegestaan (${this.data.getBestandsextensie(
          file,
        )})`;
        return;
      }

      if (!this.data.isBestandsgrootteToegestaan(file)) {
        this.data.uploadError = `Het bestand is te groot (${this.data.getBestandsgrootteMB(
          file,
        )}MB)`;
        return;
      }

      if (!file.size) {
        this.data.uploadError = "Het bestand is leeg";
        this.data.formControl.setErrors({
          emptyFile: true,
        });
        return;
      }

      this.subscription = this.createRequest(file)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (event: HttpEvent<any>) => {
            switch (event.type) {
              case HttpEventType.Sent:
                this.status = UploadStatus.BEZIG;
                break;
              case HttpEventType.ResponseHeader:
                break;
              case HttpEventType.UploadProgress:
                this.progress = Math.round((event.loaded / event.total) * 100);
                this.updateInput(`${file.name} | ${this.progress}%`);
                break;
              case HttpEventType.Response:
                this.fileInput.nativeElement.value = null;
                this.updateInput(file.name);
                this.data.fileUploaded$.next(file.name);
                this.status = UploadStatus.GEREED;
                setTimeout(() => {
                  this.progress = 0;
                }, 1500);
            }
          },
          error: (error) => {
            this.status = UploadStatus.SELECTEER_BESTAND;
            this.fileInput.nativeElement.value = null;
            this.data.uploadError = this.foutAfhandelingService.getFout(error);
          },
        });
    } else {
      this.updateInput(null);
    }
  }

  reset($event?: MouseEvent) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.subscription.closed) {
      this.subscription.unsubscribe();
    }
    this.status = UploadStatus.SELECTEER_BESTAND;
    this.fileInput.nativeElement.value = null;
    this.updateInput(null);
  }

  createRequest(file: File): Observable<any> {
    this.updateInput(file.name);
    const formData: FormData = new FormData();
    formData.append("filename", file.name);
    formData.append("filesize", file.size.toString());
    formData.append("type", file.type);
    formData.append("file", file, file.name);
    const httpHeaders = new HttpHeaders();
    httpHeaders.append("Content-Type", "multipart/form-data");
    httpHeaders.append("Accept", "application/json");
    return this.http.post(this.data.uploadURL, formData, {
      reportProgress: true,
      observe: "events",
      headers: httpHeaders,
    });
  }

  updateInput(inputValue: string) {
    this.data.formControl.setValue(inputValue);
    this.changeDetector.detectChanges();
  }

  getErrorMessage(): string {
    if (this.data.uploadError) {
      return this.data.uploadError;
    }
    return super.getErrorMessage();
  }

  droppedFile(file: File): void {
    this.uploadFile(file);
  }
}
