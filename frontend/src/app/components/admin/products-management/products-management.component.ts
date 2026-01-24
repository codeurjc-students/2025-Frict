import {Component, OnInit, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {Button} from 'primeng/button';
import {UIChart} from 'primeng/chart';
import {DropdownModule} from 'primeng/dropdown';
import {TableModule} from 'primeng/table';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {Tooltip} from 'primeng/tooltip';
import {PageResponse} from '../../../models/pageResponse.model';
import {Product} from '../../../models/product.model';
import {ProductService} from '../../../services/product.service';
import {formatPrice} from '../../../utils/numberFormat.util';
import {RouterLink} from '@angular/router';
import {Select} from 'primeng/select';
import {Tag} from 'primeng/tag';
import {getUserStatusTagInfo} from '../../../utils/tagManager.util';
import {ConfirmationService, MessageService} from 'primeng/api';


@Component({
  selector: 'app-products-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, Button, UIChart, DropdownModule, TableModule, ToggleSwitch, Paginator, Tooltip, RouterLink, Select, Tag,
  ],
  templateUrl: './products-management.component.html',
  styleUrl: 'products-management.component.css'
})
export class ProductsManagementComponent implements OnInit {

  productsPage: PageResponse<Product> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  selectedProduct: Product | null = null;

  // Pagination
  first = 0;
  rows = 10;

  loading: boolean = true;
  error: boolean = false;

  // Chart Config
  pieData = signal<any>(null);
  pieOptions = signal<any>(null);
  lineData = signal<any>(null);
  lineOptions = signal<any>(null);

  // Sales chart selector
  chartProductSelector = signal<Product | null>(null);

  constructor(private productService: ProductService,
              private confirmationService: ConfirmationService,
              private messageService: MessageService) {}

  confirmDelete(event: Event, id: string) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: 'Se eliminarán del sistema la información del producto, su stock y reseñas publicadas.',
      header: 'Borrar producto',
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: {
        label: 'Cancel',
        severity: 'secondary',
        outlined: true,
      },
      acceptButtonProps: {
        label: 'Save',
        severity: 'danger',
      },
      accept: () => {
        this.productService.deleteProduct(id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Se ha borrado el producto correctamente.' });
            this.loadProducts();
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Ha ocurrido un error borrando el producto.' });
          }
        })
      }
    });
  }

  ngOnInit() {
    this.loadProducts();
  }

  // --- Actions ---

  onGlobalAction(action: string) {
    if (action === 'activate_all') {
      this.productService.toggleAllGlobalActivations(true).subscribe({
        next: () => {
          this.productsPage.items.forEach(p => p.active = true);
        }
      });
    } else if (action === 'deactivate_all') {
      this.productService.toggleAllGlobalActivations(false).subscribe({
        next: () => {
          this.productsPage.items.forEach(p => p.active = false);
        }
      });
    }
  }


  onToggleActive(product: Product, event: any) {
    const originalValue = product.active;
    const newValue = event.checked;

    product.active = newValue;

    this.productService.toggleGlobalActivation(product.id, newValue).subscribe({
      next: () => {
        console.log('Estado actualizado correctamente');
      },
      error: () => {
        console.error('Error al actualizar, revirtiendo cambios...');
        product.active = originalValue;
      }
    });
  }

  onChartProductChange(event: any) {
    this.chartProductSelector.set(event.value);
    this.updateLineChartData();
  }

  onProductsPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadProducts();
  }

  private loadProducts() {
    this.productService.getAllProducts(this.first/this.rows, this.rows).subscribe({
      next: (products) => {
        this.productsPage = products;
        this.chartProductSelector.set(this.productsPage.items[0]);
        this.initCharts();
      },
      error: () => {
      }
    })
  }

  private initCharts() {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color') || '#334155';
    const topStockProducts = this.productsPage.items; //.slice(0, 5);

    this.pieData.set({
      labels: topStockProducts.map(p => p.name),
      datasets: [
        {
          data: topStockProducts.map(p => p.totalUnits),
          backgroundColor: ['#eab308', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899'],
          hoverBackgroundColor: ['#ca8a04', '#0891b2', '#2563eb', '#7c3aed', '#db2777']
        }
      ]
    });

    this.pieOptions.set({
      plugins: {
        legend: {
          display: topStockProducts.length <= 10,
          position: 'bottom',
          labels: {
            usePointStyle: true,
            color: textColor
          }
        }
      }
    });

    this.lineOptions.set({
      maintainAspectRatio: false,
      aspectRatio: 0.7,
      plugins: {
        legend: { labels: { color: textColor } }
      },
      scales: {
        x: { ticks: { color: '#64748b' }, grid: { color: '#e2e8f0' } },
        y: { ticks: { color: '#64748b' }, grid: { color: '#e2e8f0' } }
      }
    });

    this.updateLineChartData();
  }

  private updateLineChartData() {
    const product = this.chartProductSelector();
    if (!product) return;

    const base = +product.id * 10;
    const data = [base + 50, base + 20, base + 80, base + 45, base + 90, base + 120];

    this.lineData.set({
      labels: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun'],
      datasets: [
        {
          label: `Ventas: ${product.name}`,
          data: data,
          fill: true,
          borderColor: '#06b6d4', // Cyan
          backgroundColor: 'rgba(6, 182, 212, 0.1)',
          tension: 0.4
        }
      ]
    });
  }

  protected readonly formatPrice = formatPrice;
  protected readonly getUserStatusTagInfo = getUserStatusTagInfo;
}
