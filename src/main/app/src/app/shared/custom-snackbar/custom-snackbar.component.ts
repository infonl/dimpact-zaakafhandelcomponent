import { Component, Inject } from "@angular/core";
import {
  MAT_SNACK_BAR_DATA,
  MatSnackBarRef,
} from "@angular/material/snack-bar";
import { MatIconModule } from "@angular/material/icon";

@Component({
  selector: "app-custom-snackbar",
  template: `
    <div class="party-snackbar">
      <div class="message">{{ data.message }}</div>
      <div class="actions">
        <button mat-button *ngIf="data.action" (click)="onAction()">
          {{ data.action }}
        </button>
        <button mat-icon-button aria-label="Close" (click)="onClose()">
          <mat-icon class="close-icon">close</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .party-snackbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
        padding: 16px;
        color: #fff;
        background: linear-gradient(135deg, #ff4081, #7c4dff, #18ffff);
        border-radius: 12px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        animation: fadeIn 0.3s ease-in;
      }

      .message {
        font-weight: 500;
        font-size: 16px;
      }

      .actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .close-icon {
        color: black;
      }

      button.mat-icon-button {
        background-color: transparent !important;
      }

      button.mat-icon-button:hover,
      button.mat-icon-button:focus {
        background-color: transparent !important;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
    `,
  ],
  standalone: true,
  imports: [MatIconModule],
})
export class CustomSnackbarComponent {
  constructor(
    @Inject(MAT_SNACK_BAR_DATA) public data: any,
    private snackBarRef: MatSnackBarRef<CustomSnackbarComponent>,
  ) {}

  onAction(): void {
    this.snackBarRef.dismissWithAction();
  }

  onClose(): void {
    this.snackBarRef.dismiss();
  }
}
