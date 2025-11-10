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
import {LoadingComponent} from '../../common/loading/loading.component';

@Component({
  selector: 'app-client-home',
  standalone: true,
  imports: [CommonModule, NavbarComponent, ButtonModule, RouterLink, Carousel, FooterComponent, ProductCardComponent, LoadingComponent],
  templateUrl: './client-home.component.html',
  styleUrls: ['./client-home.component.css']
})
export class ClientHomeComponent implements OnInit {

  protected readonly responsiveOptions = responsiveOptions;

  featuredProducts: Product[] = [];
  recommendedProducts: Product[] = [];
  topSalesProducts: Product[] = [];

  featuredLoading: boolean = true;
  featuredError: boolean = false;

  recommendedLoading: boolean = true;
  recommendedError: boolean = false;

  topSalesLoading: boolean = true;
  topSalesError: boolean = false;

  constructor(private productService: ProductService) {}

  ngOnInit() {
    this.productService.getProductsByCategoryName("Destacado").subscribe({
      next: (products) => {
        this.featuredProducts = products.products;
        this.featuredLoading = false;
      },
      error: (error) => {
        this.featuredLoading = false;
        this.featuredError = true;
      }
    })

    this.productService.getProductsByCategoryName("Top ventas").subscribe({
      next: (products) => {
        this.topSalesProducts = products.products;
        this.topSalesLoading = false;
      },
      error: (error) => {
        this.topSalesLoading = false;
        this.topSalesError = true;
      }
    })

    this.productService.getProductsByCategoryName("Recomendado").subscribe({
      next: (products) => {
        this.recommendedProducts = products.products;
        this.recommendedLoading = false;
      },
      error: (error) => {
        this.recommendedLoading = false;
        this.recommendedError = true;
      }
    })
  }
}
