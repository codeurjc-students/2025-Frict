import {Component, computed, inject, input} from '@angular/core';
import {Tag} from 'primeng/tag';
import {Product} from '../../../models/product.model';
import {getStockTagInfo} from '../../../utils/tagManager.util';
import {ProductService} from '../../../services/product.service';

@Component({
  selector: 'app-stock-tag',
  standalone: true,
  imports: [
    Tag
  ],
  templateUrl: 'stock-tag.component.html',
  styleUrl: 'stock-tag.component.css'
})
export class StockTagComponent {
  private productService = inject(ProductService);

  product = input.required<Product>();

  tagInfo = computed(() => {
    const p = this.product();
    const isLocal = this.productService.searchScope() === 'LOCAL';
    const units = isLocal ? p.availableUnits : p.totalUnits;
    return getStockTagInfo(units, isLocal);
  });
}
