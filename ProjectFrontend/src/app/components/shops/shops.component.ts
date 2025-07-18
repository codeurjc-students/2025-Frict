import { Component } from '@angular/core';
import {ProductCardComponent} from '../product-card/product-card.component';

@Component({
  selector: 'app-shops',
  imports: [
    ProductCardComponent
  ],
  templateUrl: './shops.component.html',
  standalone: true,
  styleUrl: './shops.component.css'
})
export class ShopsComponent {

}
