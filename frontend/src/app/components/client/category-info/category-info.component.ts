import {Component, inject, LOCALE_ID, OnInit} from '@angular/core';
import {CommonModule, formatDate} from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { RatingModule } from 'primeng/rating';
import { FormsModule } from '@angular/forms';
import { ChartModule } from 'primeng/chart';
import { DropdownModule } from 'primeng/dropdown';

import { ProductCardComponent } from '../product-card/product-card.component';
import { Product } from '../../../models/product.model';
import { CategoryService } from '../../../services/category.service';
import { ProductService } from '../../../services/product.service';
import { Category } from '../../../models/category.model';
import { BreadcrumbReloadComponent } from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import { BreadcrumbService } from '../../../utils/breadcrumb.service';
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { LoadingSectionComponent } from '../../common/loading-section/loading-section.component';
import { carouselResponsiveOptions } from '../../../app.config';
import {RegistryService} from '../../../services/registry.service';
import { SafeHtmlPipe } from '../../../utils/safe-html.pipe';
import { StockTagComponent } from '../../common/stock-tag/stock-tag.component';

@Component({
  selector: 'app-category-info',
  standalone: true,
  imports: [
    CommonModule,
    SafeHtmlPipe,
    StockTagComponent,
    RouterModule,
    ButtonModule,
    TagModule,
    RatingModule,
    FormsModule,
    ChartModule,
    DropdownModule,
    ProductCardComponent,
    BreadcrumbReloadComponent,
    LoadingScreenComponent,
    LoadingSectionComponent
  ],
  templateUrl: './category-info.component.html',
  styleUrl: './category-info.component.css'
})
export class CategoryInfoComponent implements OnInit {

  private categoryService = inject(CategoryService);
  private productService = inject(ProductService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private breadcrumbService = inject(BreadcrumbService);
  private locale = inject(LOCALE_ID);

  readonly tagSeverities: Array<'success' | 'info' | 'warn' | 'danger' | 'secondary'> = ['success', 'info', 'warn', 'danger', 'secondary'];

  // Estados generales
  loading: boolean = true;
  error: boolean = false;

  // Datos estructurales
  mainCategory!: Category;
  similarCategories: Category[] = [];
  subCategories: Category[] = [];

  // Expositor de Top Ventas
  topSalesProducts: Product[] = [];
  topSalesLoading: boolean = true;
  topSalesError: boolean = false;
  protected readonly responsiveOptions = carouselResponsiveOptions;

  // Métricas de la categoría
  totalSalesCount: number = 0;
  totalViewsCount: number = 0;
  totalShopsCount: number = 0;
  totalProductsCount: number = 0;


  // Gráficas y Rangos de tiempo
  viewsChartData: any;
  salesChartData: any;

  timeRanges = [
    { label: 'Últimos 7 días', value: 7 },
    { label: 'Últimos 30 días', value: 30 },
    { label: 'Últimos 90 días', value: 90 }
  ];
  selectedViewsRange: number = 30;
  selectedSalesRange: number = 30;

  // Estilo "Clean" para las gráficas (Líneas rectas, sin grid pesado)
  chartOptions = {
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1e293b',
        titleFont: { size: 12, weight: 'bold' },
        bodyFont: { size: 12 },
        padding: 10,
        displayColors: false
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#94a3b8', font: { size: 10 } }
      },
      y: {
        border: { display: false },
        grid: { color: '#f1f5f9', drawTicks: false },
        ticks: { color: '#94a3b8', font: { size: 10 } }
      }
    },
    elements: {
      line: {
        tension: 0, // 0 = Líneas totalmente rectas
        borderWidth: 2,
        borderColor: '#3b82f6'
      },
      point: {
        radius: 4,
        backgroundColor: '#3b82f6',
        hoverRadius: 6,
        borderWidth: 0
      }
    },
    maintainAspectRatio: false
  };

  ngOnInit() {
    this.route.params.subscribe(() => {
      this.loadMainCategory();
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  loadMainCategory() {
    this.loading = true;
    this.error = false;
    const id = this.route.snapshot.paramMap.get('id');
    const navState = typeof history !== 'undefined' ? history.state : {};

    if (id) {
      this.categoryService.getCategoryById(id).subscribe({
        next: (category) => {
          this.mainCategory = category;
          this.subCategories = category.children || [];

          const currentUrl = this.router.url;
          this.breadcrumbService.setNodesForUrl(currentUrl, [{ label: category.name }]);

          if (navState.from === 'categories-management') {
            this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
              { label: 'Gestor de Categorías', routerLink: '/admin/categories' }
            ]);
          } else {
            this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, []);
          }

          this.loadSimilarCategories();
          this.loadCategoryMetrics(category.id);
          this.loadChartsData();
          this.loadTopSalesProducts(category.id);

          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    }
  }

  loadSimilarCategories() {
    if (this.mainCategory.parentId) {
      this.categoryService.getCategoryById(this.mainCategory.parentId).subscribe({
        next: (category) => {
          this.similarCategories = category.children.filter(c => c.id !== this.mainCategory.id);
        }
      });
    } else {
      this.categoryService.getAllCategories().subscribe({
        next: (c) => {
          this.similarCategories = c.filter(cat => cat.id !== this.mainCategory.id).slice(0, 4);
        }
      });
    }
  }

  loadCategoryMetrics(categoryId: string) {
    this.productService.getCategoryMetrics(categoryId).subscribe({
      next: (metrics: any) => {
        // Mapeamos los datos reales combinados de la base de datos
        this.totalSalesCount = metrics.totalSales || 0;
        this.totalViewsCount = metrics.totalViews || 0;
        this.totalShopsCount = metrics.totalShops || 0;
        this.totalProductsCount = metrics.totalProducts || 0;
      },
      error: (err) => {
        console.error("Error al cargar las métricas de la categoría:", err);
      }
    });
  }

  loadChartsData() {
    this.loadViewsChart();
    this.loadSalesChart();
  }

  loadViewsChart() {
    this.productService.getCategoryTimeline(this.mainCategory.id.toString(), 'PRODUCT_VIEWS', this.selectedViewsRange).subscribe({
      next: (res: any[]) => {
        console.log(res);
        const labels = res.map(item => formatDate(item._id, 'dd MMM yyyy', this.locale));
        const dataValues = res.map(item => item.value || item.totalValue || 0);

        this.viewsChartData = {
          labels: labels,
          datasets: [{
            data: dataValues,
            fill: false,
            borderColor: '#3b82f6',
            backgroundColor: '#3b82f6',
            borderWidth: 2,
            tension: 0
          }]
        };
      },
      error: (err) => console.error("Error cargando gráfica de visualizaciones", err)
    });
  }

  loadSalesChart() {
    this.productService.getCategoryTimeline(this.mainCategory.id.toString(), 'PRODUCT_UNITS_SOLD', this.selectedSalesRange).subscribe({
      next: (res: any[]) => {
        console.log(res);
        const labels = res.map(item => formatDate(item._id, 'dd MMM yyyy', this.locale));
        const dataValues = res.map(item => item.value || item.totalValue || 0);

        this.salesChartData = {
          labels: labels,
          datasets: [{
            data: dataValues,
            fill: false,
            borderColor: '#10b981',
            backgroundColor: '#10b981',
            borderWidth: 2,
            tension: 0
          }]
        };
      },
      error: (err) => console.error("Error cargando gráfica de ventas", err)
    });
  }

  onViewsRangeChange() { this.loadViewsChart(); }
  onSalesRangeChange() { this.loadSalesChart(); }

  loadTopSalesProducts(categoryId: string) {
    this.topSalesLoading = true;
    this.topSalesError = false;

    this.productService.getCategoryTopSales(categoryId, 10).subscribe({
      next: (pageResponse: any) => {
        // Asignamos directamente la propiedad 'items' que vemos en tu captura de consola
        this.topSalesProducts = pageResponse.items || [];
        this.topSalesLoading = false;
      },
      error: (err) => {
        console.error('Error al cargar Top Ventas:', err);
        this.topSalesError = true;
        this.topSalesLoading = false;
      }
    });
  }
}
