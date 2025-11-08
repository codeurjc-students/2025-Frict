import {Component, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {ButtonModule} from 'primeng/button';
import {RouterLink} from '@angular/router';
import {Carousel} from 'primeng/carousel';
import {responsiveOptions} from '../../../app.config';
import {FooterComponent} from '../../common/footer/footer.component';
import {ProductCardComponent} from '../product-card/product-card.component';
import {ProductsPage} from '../../../models/productsPage.model';
import {Product} from '../../../models/product.model';
import {ProductService} from '../../../services/product.service';

@Component({
  selector: 'app-client-home',
  standalone: true,
  imports: [CommonModule, NavbarComponent, ButtonModule, RouterLink, Carousel, FooterComponent, ProductCardComponent],
  templateUrl: './client-home.component.html',
  styleUrls: ['./client-home.component.css']
})
export class ClientHomeComponent implements OnInit {

  protected readonly responsiveOptions = responsiveOptions;

  featuredProducts: Product[] = [];
  recommendedProducts: Product[] = [];
  topSalesProducts: Product[] = [];

  constructor(private productService: ProductService) {}

  ngOnInit() {
    this.productService.getProductsByCategoryName("Destacado").subscribe({
      next: (products) => {
          this.featuredProducts = products.products;
      }
    })
    this.productService.getProductsByCategoryName("Top ventas").subscribe({
      next: (products) => {
        this.topSalesProducts = products.products;
      }
    })
    this.productService.getProductsByCategoryName("Recomendado").subscribe({
      next: (products) => {
        this.recommendedProducts = products.products;
      }
    })
  }
}
