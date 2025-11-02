import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {ButtonModule} from 'primeng/button';
import {RouterLink} from '@angular/router';
import {Carousel} from 'primeng/carousel';
import {responsiveOptions} from '../../../app.config';
import {FooterComponent} from '../../common/footer/footer.component';
import {ProductCardComponent} from '../product-card/product-card.component';

@Component({
  selector: 'app-client-home',
  standalone: true,
  imports: [CommonModule, NavbarComponent, ButtonModule, RouterLink, Carousel, FooterComponent, ProductCardComponent],
  templateUrl: './client-home.component.html',
  styleUrls: ['./client-home.component.css']
})
export class ClientHomeComponent {

  products = [
    { id: 1, name: 'Producto 1', price: '499', discount: '-10%', image: '/assets/laptop.png', inventoryStatus: 'INSTOCK' },
    { id: 2, name: 'Producto 2', price: '189', discount: '-15%', image: '/assets/monitor.png', inventoryStatus: 'INSTOCK'  },
    { id: 3, name: 'Producto 3', price: '699', discount: '-20%', image: '/assets/gpu.png', inventoryStatus: 'INSTOCK'  },
    { id: 4, name: 'Producto 4', price: '79', discount: '-5%', image: '/assets/keyboard.png', inventoryStatus: 'INSTOCK'  },
    { id: 5, name: 'Producto 5', price: '499', discount: '-10%', image: '/assets/laptop.png', inventoryStatus: 'INSTOCK' },
    { id: 6, name: 'Producto 6', price: '189', discount: '-15%', image: '/assets/monitor.png', inventoryStatus: 'INSTOCK'  },
    { id: 7, name: 'Producto 7', price: '699', discount: '-20%', image: '/assets/gpu.png', inventoryStatus: 'INSTOCK'  },
    { id: 8, name: 'Producto 8', price: '79', discount: '-5%', image: '/assets/keyboard.png', inventoryStatus: 'INSTOCK'  }
  ];

  /*constructor(private productService: ProductService) {}

  ngOnInit() {
    this.productService.getProductsSmall().then((products) => {
      this.products = products;
    });
  }

   */

  protected readonly responsiveOptions = responsiveOptions;
}
