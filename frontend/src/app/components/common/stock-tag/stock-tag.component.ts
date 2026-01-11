import {Component, Input} from '@angular/core';
import {Tag} from 'primeng/tag';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-stock-tag',
  imports: [
    Tag,
    NgIf
  ],
  templateUrl: './stock-tag.component.html',
  styleUrl: './stock-tag.component.css'
})
export class StockTagComponent {

  @Input() units: number = 0;

  protected getStockTagMessage(): string {
    if (this.units <= 10 && this.units > 5) {
      return 'Quedan ' + this.units;
    } else if (this.units <= 5 && this.units > 0) {
      return '¡Quedan ' + this.units + '!';
    } else {
      return 'Agotado';
    }
  }

  protected getStockTagIcon(): string {
    if (this.units <= 10 && this.units > 5) {
      return 'pi pi-info-circle';
    } else if (this.units <= 5 && this.units > 0) {
      return 'pi pi-exclamation-triangle';
    } else {
      return 'pi pi-times';
    }
  }

  protected getSeverity(): string {
    if (this.units <= 10 && this.units > 5) {
      return 'info';
    } else if (this.units <= 5 && this.units > 0) {
      return 'warn';
    } else {
      return 'danger';
    }
  }
}
