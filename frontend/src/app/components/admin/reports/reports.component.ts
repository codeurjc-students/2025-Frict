import { Component, effect, inject, signal, computed, OnInit, LOCALE_ID } from '@angular/core';
import { CommonModule, formatDate } from '@angular/common';
import { FormsModule } from '@angular/forms';

// PrimeNG 19
import { Select } from 'primeng/select';
import { MultiSelect } from 'primeng/multiselect';
import { DatePicker } from 'primeng/datepicker'; // Nuevo en V19 (reemplaza a Calendar)
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { ChartModule } from 'primeng/chart'; // Para integrar Chart.js

import { RegistryService } from '../../../services/registry.service';
import { BreadcrumbReloadComponent } from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    BreadcrumbReloadComponent, LoadingScreenComponent,
    Select, MultiSelect, DatePicker, Button, Tag, TableModule, ChartModule
  ],
  templateUrl: './reports.component.html'
})
export class ReportsComponent implements OnInit {
  private registryService = inject(RegistryService);
  private locale = inject(LOCALE_ID); // Para formatear las etiquetas de la gráfica

  loading = false;
  error = false;

  // --- SELECCIONES DE FECHA Y VISTA (NUEVO) ---
  // Por defecto: últimos 30 días
  startDate = signal<Date | null>(new Date(new Date().setDate(new Date().getDate() - 30)));
  endDate = signal<Date | null>(new Date());
  selectedInterval = signal<string>('week');

  intervalOptions = signal([
    { label: 'Diario', value: 'day' },
    { label: 'Semanal', value: 'week' },
    { label: 'Mensual', value: 'month' },
    { label: 'Anual', value: 'year' }
  ]);

  // --- SELECCIONES DEL USUARIO ---
  selectedEntity = signal<string | null>(null);
  selectedMetric = signal<string | null>(null);
  selectedProducts = signal<string[]>([]);
  selectedStores = signal<string[]>([]);
  selectedOrders = signal<string[]>([]);
  selectedUsers = signal<string[]>([]);

  // --- CATÁLOGOS ---
  entityOptions = signal<string[]>([]);
  metricOptions = signal<string[]>([]);
  productOptions = signal<string[]>([]);
  storeOptions = signal<string[]>([]);
  orderOptions = signal<string[]>([]);
  userOptions = signal<string[]>([]);

  // --- ESTADO DE LA VISTA (Derecha) ---
  chartType = signal<'bar' | 'line' | 'pie'>('bar');
  isLoadingReport = signal<boolean>(false);

  graphData = signal<any[]>([]);
  tableData = signal<any[]>([]);

  // --- LÓGICA DE GRÁFICAS (CHART.JS + PRIMENG) ---
  chartData = computed(() => {
    const rawData = this.graphData();
    if (!rawData || rawData.length === 0) return {};

    // Mapeamos las fechas a etiquetas legibles
    const labels = rawData.map(item => formatDate(item._id, 'dd MMM yyyy', this.locale));
    const dataValues = rawData.map(item => item.totalValue);

    // Si es tipo Pie, necesitamos múltiples colores
    const isPie = this.chartType() === 'pie';
    const bgColors = isPie
      ? ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6']
      : 'rgba(59, 130, 246, 0.5)'; // Azul para barras/líneas
    const borderColors = isPie ? bgColors : 'rgb(59, 130, 246)';

    return {
      labels: labels,
      datasets: [
        {
          label: this.selectedMetric() || 'Total',
          data: dataValues,
          backgroundColor: bgColors,
          borderColor: borderColors,
          borderWidth: 2,
          fill: true,
          tension: 0.4 // Para que las líneas sean curvas
        }
      ]
    };
  });

  // Opciones dinámicas (Oculta ejes si es un gráfico circular)
  chartOptions = computed(() => {
    const isPie = this.chartType() === 'pie';
    return {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        legend: { display: isPie, position: 'right' },
        tooltip: { mode: 'index', intersect: false }
      },
      scales: {
        x: { display: !isPie }, // Oculta el eje X en gráficas circulares
        y: { display: !isPie, beginAtZero: true } // Oculta el eje Y en circulares
      }
    };
  });

  tableColumns = computed(() => {
    const data = this.tableData();
    if (data.length === 0) return [];

    const sampleItem = data[0];
    const columns = ['timestamp'];
    if (sampleItem.metrics?.value !== undefined) columns.push('value');
    if (sampleItem.metadata) {
      Object.keys(sampleItem.metadata).forEach(key => columns.push(`meta_${key}`));
    }
    return columns;
  });

  ngOnInit() {
    this.loading = true;
    this.registryService.getEntityTypes().subscribe({
      next: (entities) => {
        this.entityOptions.set(entities);
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  constructor() {
    effect(() => {
      const entity = this.selectedEntity();
      if (entity) {
        this.selectedMetric.set(null);
        this.resetAssociatedEntities();

        this.registryService.getMetrics(entity).subscribe({
          next: (res) => this.metricOptions.set(res),
          error: () => this.metricOptions.set([])
        });
      } else {
        this.metricOptions.set([]);
      }
    });

    effect(() => {
      const metric = this.selectedMetric();
      const entity = this.selectedEntity();
      if (metric && entity) {
        this.resetAssociatedEntities();

        this.registryService.getCrossReferences(entity, metric).subscribe({
          next: (referencesMap) => {
            this.storeOptions.set(referencesMap.storeId || []);
            this.productOptions.set(referencesMap.productId || []);
            this.userOptions.set(referencesMap.userId || []);
            this.orderOptions.set(referencesMap.orderId || []);
          },
          error: () => this.resetAssociatedEntities()
        });
      }
    });
  }

  private resetAssociatedEntities() {
    this.selectedProducts.set([]);
    this.selectedStores.set([]);
    this.selectedOrders.set([]);
    this.selectedUsers.set([]);
    this.productOptions.set([]);
    this.storeOptions.set([]);
    this.orderOptions.set([]);
    this.userOptions.set([]);
  }

  fetchReportData() {
    const entity = this.selectedEntity();
    const metric = this.selectedMetric();
    const start = this.startDate();
    const end = this.endDate();

    // Nueva validación con fechas incluidas
    if (!entity || !metric || !start || !end) return;

    this.isLoadingReport.set(true);

    const baseParams = {
      startDate: start.toISOString(), // Enviamos las fechas reales
      endDate: end.toISOString(),
      entityType: entity,
      dataType: metric,
      productIds: this.selectedProducts(),
      storeIds: this.selectedStores(),
      orderIds: this.selectedOrders(),
      userIds: this.selectedUsers()
    };

    // Petición 1: Gráfica (usando el intervalo dinámico)
    this.registryService.loadRegistry({ ...baseParams, viewType: 'GRAPH', interval: this.selectedInterval() } as any)
      .subscribe({
        next: (res) => this.graphData.set(res),
        error: () => this.graphData.set([])
      });

    // Petición 2: Tabla
    this.registryService.loadRegistry({ ...baseParams, viewType: 'TABLE' } as any)
      .subscribe({
        next: (res) => {
          this.tableData.set(res);
          this.isLoadingReport.set(false);
        },
        error: () => {
          this.tableData.set([]);
          this.isLoadingReport.set(false);
        }
      });
  }

  getCellValue(row: any, column: string): any {
    if (column === 'timestamp') return row.timestamp;
    if (column === 'value') return row.metrics?.value;
    if (column.startsWith('meta_')) {
      const metaKey = column.replace('meta_', '');
      return row.metadata?.[metaKey] || '-';
    }
    return '-';
  }

  resetAll() {
    this.startDate.set(new Date(new Date().setDate(new Date().getDate() - 30)));
    this.endDate.set(new Date());
    this.selectedInterval.set('week');
    this.selectedEntity.set(null);
    this.selectedMetric.set(null);
    this.resetAssociatedEntities();
    this.graphData.set([]);
    this.tableData.set([]);
  }
}
