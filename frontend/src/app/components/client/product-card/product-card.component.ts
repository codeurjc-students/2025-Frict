import {Component, computed, Input} from '@angular/core';
import {RouterLink} from "@angular/router";
import {Product} from '../../../models/product.model';
import {NgIf} from '@angular/common';
import {formatPrice, formatRating} from '../../../utils/textFormat.util';
import {Tag} from 'primeng/tag';
import {getStockTagInfo} from '../../../utils/tagManager.util';
import {AuthService} from '../../../services/auth.service';
import {ProductService} from '../../../services/product.service';

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

  constructor(private productService: ProductService) {
  }


  localMode = computed(() => this.productService.searchScope() === 'LOCAL');
  stockStatus = computed(() => {
    const units = this.localMode() ? this.product.availableUnits : this.product.totalUnits;
    if (units > 10) {
      return null;
    }
    return getStockTagInfo(units, this.localMode());
  });

  protected readonly formatPrice = formatPrice;
  protected readonly formatRating = formatRating;
}
