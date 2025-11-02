import {Component, Input} from '@angular/core';
import {RouterLink} from "@angular/router";
import {Tag} from "primeng/tag";

@Component({
  selector: 'app-product-card',
    imports: [
        RouterLink,
        Tag
    ],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.css'
})
export class ProductCardComponent {

  @Input() product!: any;

  getSeverity(status: string) {
    switch (status) {
      case 'INSTOCK':
        return 'success';
      case 'LOWSTOCK':
        return 'warn';
      case 'OUTOFSTOCK':
        return 'danger';
      default:
        return 'danger';
    }
  }

  protected addToFavourites() {
    alert("Añadido a favoritos")
  }

  protected addToCart() {
    alert("Añadido a la cesta")
  }
}
