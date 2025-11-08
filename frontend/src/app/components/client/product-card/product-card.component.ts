import {Component, Input} from '@angular/core';
import {RouterLink} from "@angular/router";
import {Tag} from "primeng/tag";
import {Product} from '../../../models/product.model';
import {NgIf} from '@angular/common';
import {formatPrice, formatRating} from '../../../utils/numberFormat.util';

@Component({
  selector: 'app-product-card',
  imports: [
    RouterLink,
    Tag,
    NgIf
  ],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.css'
})
export class ProductCardComponent {

  @Input() product!: Product;

  getStockMessage():string {
    let units = this.product.availableUnits;
    if (units <= 10 && units > 5) {
      return 'Quedan ' + units;
    } else if (units <= 5 && units > 0) {
      return '¡Sólo quedan ' + units + '!';
    } else {
      return 'Agotado';
    }
  }

  protected addToFavourites() {
    alert("Añadido a favoritos")
  }

  protected addToCart() {
    alert("Añadido a la cesta")
  }

  protected readonly formatPrice = formatPrice;
  protected readonly formatNumber = formatPrice;
  protected readonly formatRating = formatRating;
}
