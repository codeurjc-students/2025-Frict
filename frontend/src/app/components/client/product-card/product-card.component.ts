import {Component, computed, Input} from '@angular/core';
import {RouterLink} from "@angular/router";
import {Product} from '../../../models/product.model';
import {NgIf} from '@angular/common';
import {formatPrice, formatRating} from '../../../utils/numberFormat.util';
import {Tag} from 'primeng/tag';
import {getStockTagInfo} from '../../../utils/tagManager.util';

@Component({
  selector: 'app-product-card',
  imports: [
    RouterLink,
    NgIf,
    Tag
  ],
  templateUrl: './product-card.component.html',
  standalone: true,
  styleUrl: './product-card.component.css'
})
export class ProductCardComponent {

  @Input() product!: Product;

  @Input() elementId: string = 'product';

  stockStatus = computed(() => getStockTagInfo(this.product.totalUnits));

  protected readonly formatPrice = formatPrice;
  protected readonly formatRating = formatRating;
}
