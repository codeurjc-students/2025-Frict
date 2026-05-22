import {Component, Input} from '@angular/core';
import {RouterLink} from "@angular/router";
import {Product} from '../../../models/product.model';
import {formatPrice, formatRating} from '../../../utils/textFormat.util';
import {StockTagComponent} from '../../common/stock-tag/stock-tag.component';
import {Tag} from 'primeng/tag';


@Component({
  selector: 'app-product-card',
  imports: [
    RouterLink,
    StockTagComponent,
    Tag
  ],
  templateUrl: './product-card.component.html',
  standalone: true,
  styleUrl: './product-card.component.css'
})
export class ProductCardComponent {

  @Input() product!: Product;

  @Input() elementId: string = 'product';

  @Input() navState: Record<string, string> = {};

  constructor() {
  }

  protected readonly formatPrice = formatPrice;
  protected readonly formatRating = formatRating;
}
