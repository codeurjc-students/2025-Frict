import {Component, Input} from '@angular/core';
import {RouterLink} from "@angular/router";
import {Tag} from "primeng/tag";
import {Product} from '../../../models/product.model';
import {NgIf} from '@angular/common';
import {formatPrice, formatRating} from '../../../utils/numberFormat.util';
import {StockTagComponent} from '../../common/stock-tag/stock-tag.component';

@Component({
  selector: 'app-product-card',
  imports: [
    RouterLink,
    Tag,
    NgIf,
    StockTagComponent
  ],
  templateUrl: './product-card.component.html',
  standalone: true,
  styleUrl: './product-card.component.css'
})
export class ProductCardComponent {

  @Input() product!: Product;

  @Input() elementId: string = 'product';

  protected readonly formatPrice = formatPrice;
  protected readonly formatRating = formatRating;
}
