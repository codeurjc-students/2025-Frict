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

interface CategoryHomeUI {
  icon?: string;
  label?: string;
  description?: string;
}

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
    LoadingSectionComponent
  ],
  templateUrl: './client-home.component.html',
  styleUrls: ['./client-home.component.css']
})
export class ClientHomeComponent implements OnInit {

  protected readonly responsiveOptions = carouselResponsiveOptions;

  // --- CONFIGURACIÓN VISUAL (NUEVO) ---

  // 1. Configuración de Servicios (Envío, Garantía, etc.)
  public services: ServiceUI[] = [
    { icon: 'pi pi-truck',      title: 'Envío Gratis',    subtitle: 'En pedidos +50€' },
    { icon: 'pi pi-shield',     title: 'Garantía 3 años', subtitle: '100% oficial' },
    { icon: 'pi pi-undo',       title: 'Devoluciones',    subtitle: '30 días gratis' },
    { icon: 'pi pi-headphones', title: 'Soporte 24/7',    subtitle: 'Ayuda experta' }
  ];

  // Diccionario de configuración visual para las categorías
  // noinspection JSNonASCIINames
  private readonly categoryConfig: Record<string, CategoryHomeUI> = {
    'Hogar Inteligente': { icon: 'pi pi-home' /*, label: 'Gaming' */ }, //Prints the category with both custom icon and label
    'Fotografía y Video': { icon: 'pi pi-camera' },
    'Televisión e Imagen': { icon: 'pi pi-desktop' },
    'Periféricos': { icon: 'pi pi-headphones' },
    'Herramientas y Accesorios': { icon: 'pi pi-wrench' }, // Prints the category with the custom icon and the category name
    //'Audio y Sonido': {} //Prints the category, but use the default icon and the category name
  };

  public getCategoryUI(categoryName: string): CategoryHomeUI | undefined {
    const config = this.categoryConfig[categoryName];

    if (!config) {
      return { icon: 'pi pi-tag', label: categoryName ?? 'Categoría', description: ''};
    }
    else {
      return { icon: config.icon ?? 'pi pi-tag', label: config.label ?? categoryName ?? 'Categoría', description: config.description ?? ''};
    }
  }

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
    console.log("loadRecommendedProducts");
    this.productService.getProductsByCategoryName("Recomendado").subscribe({
      next: (products) => {
        this.recommendedProducts = products.items;
        console.log(products);
        const recommendedCategory = this.categories.find(c => c.name.toLowerCase() === 'recomendado');
        this.recommendedCategoryId = recommendedCategory ? recommendedCategory.id : '0';
        this.recommendedLoading = false;
      },
      error: (error) => {
        console.log("Error loadRecommendedProducts: " + error);
        this.recommendedLoading = false;
        this.recommendedError = true;
      }
    });
  }
}
