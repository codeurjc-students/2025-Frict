import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ButtonModule} from 'primeng/button';
import {RouterLink} from '@angular/router';
import {Carousel} from 'primeng/carousel';
import {carouselResponsiveOptions} from '../../../app.config';
import {ProductCardComponent} from '../product-card/product-card.component';
import {Product} from '../../../models/product.model';
import {ProductService} from '../../../services/product.service';
import {LoadingSectionComponent} from '../../common/loading-section/loading-section.component';
import {CategoryService} from '../../../services/category.service';
import {Category} from '../../../models/category.model';
import {FormsModule} from '@angular/forms';

interface ServiceUI {
  icon: string;
  title: string;
  subtitle: string;
}

@Component({
  selector: 'app-client-home',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    RouterLink,
    Carousel,
    ProductCardComponent,
    LoadingSectionComponent,
    FormsModule
  ],
  templateUrl: './client-home.component.html',
  styleUrls: ['./client-home.component.css']
})
export class ClientHomeComponent implements OnInit {

  protected readonly responsiveOptions = carouselResponsiveOptions;

  public services: ServiceUI[] = [
    { icon: 'pi pi-truck',      title: 'Envío Gratis',    subtitle: 'En pedidos +50€' },
    { icon: 'pi pi-shield',     title: 'Garantía 3 años', subtitle: '100% oficial' },
    { icon: 'pi pi-undo',       title: 'Devoluciones',    subtitle: '30 días gratis' },
    { icon: 'pi pi-headphones', title: 'Soporte 24/7',    subtitle: 'Ayuda experta' }
  ];

  categories: Category[] = [];
  featuredCategoryId: string = '0';
  recommendedCategoryId: string = '0';
  topSalesCategoryId: string = '0';
  peripheralsCategoryId: string = '0';

  featuredProducts: Product[] = [];
  recommendedProducts: Product[] = [];
  topSalesProducts: Product[] = [];

  featuredLoading: boolean = true;
  featuredError: boolean = false;

  recommendedLoading: boolean = true;
  recommendedError: boolean = false;

  topSalesLoading: boolean = true;
  topSalesError: boolean = false;

  constructor(private productService: ProductService,
              private categoryService: CategoryService) {}

  ngOnInit() {
    this.loadCategories();
  }

  private loadCategories(){
    this.categoryService.getAllCategories().subscribe({
      next: (list) => {
        this.categories = list;
        const peripheralsCategory = this.categories.find(c => c.name.toLowerCase() === 'periféricos');
        this.peripheralsCategoryId = peripheralsCategory ? peripheralsCategory.id : '0';

        this.loadFeaturedProducts();
        this.loadTopSalesProducts();
        this.loadRecommendedProducts();
      }
    })
  }

  private loadFeaturedProducts() {
    this.productService.getProductsByCategoryName("Destacado").subscribe({
      next: (products) => {
        this.featuredProducts = products.items;
        const featuredCategory = this.categories.find(c => c.name.toLowerCase() === 'destacado');
        this.featuredCategoryId = featuredCategory ? featuredCategory.id : '0';
        this.featuredLoading = false;
      },
      error: () => {
        this.featuredLoading = false;
        this.featuredError = true;
      }
    });
  }

  private loadTopSalesProducts() {
    this.productService.getProductsByCategoryName("Top Ventas").subscribe({
      next: (products) => {
        this.topSalesProducts = products.items;
        const topSalesCategory = this.categories.find(c => c.name.toLowerCase() === 'top ventas');
        this.topSalesCategoryId = topSalesCategory ? topSalesCategory.id : '0';
        this.topSalesLoading = false;
      },
      error: () => {
        this.topSalesLoading = false;
        this.topSalesError = true;
      }
    });
  }

  private loadRecommendedProducts() {
    this.productService.getProductsByCategoryName("Recomendado").subscribe({
      next: (products) => {
        this.recommendedProducts = products.items;
        const recommendedCategory = this.categories.find(c => c.name.toLowerCase() === 'recomendado');
        this.recommendedCategoryId = recommendedCategory ? recommendedCategory.id : '0';
        this.recommendedLoading = false;
      },
      error: (error) => {
        this.recommendedLoading = false;
        this.recommendedError = true;
      }
    });
  }
}
