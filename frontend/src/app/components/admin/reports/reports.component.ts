import {Component, effect, inject, signal, computed, OnInit, LOCALE_ID, ViewChild} from '@angular/core';
import { CommonModule, formatDate } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Select } from 'primeng/select';
import { MultiSelect } from 'primeng/multiselect';
import { DatePicker } from 'primeng/datepicker';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import {ChartModule, UIChart} from 'primeng/chart';

import { RegistryService } from '../../../services/registry.service';
import { BreadcrumbReloadComponent } from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import {SelectButton} from 'primeng/selectbutton';
import {Chart} from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';

Chart.register(ChartDataLabels);

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    BreadcrumbReloadComponent, LoadingScreenComponent,
    Select, MultiSelect, DatePicker, Button, TableModule, ChartModule, SelectButton
  ],
  templateUrl: './reports.component.html'
})
export class ReportsComponent implements OnInit {
  @ViewChild('myChart') chartComponent!: UIChart;

  private registryService = inject(RegistryService);
  private locale = inject(LOCALE_ID);

  loading = false;
  error = false;

  startDate = signal<Date | null>(new Date(new Date().setDate(new Date().getDate() - 30)));
  endDate = signal<Date | null>(new Date());
  selectedInterval = signal<string>('week');

  intervalOptions = signal([
    { label: 'Diario', value: 'day' },
    { label: 'Semanal', value: 'week' },
    { label: 'Mensual', value: 'month' },
    { label: 'Anual', value: 'year' }
  ]);

  metricModeOptions = signal([
    { label: 'Variación', value: 'VALUE', icon: 'pi pi-arrows-v' },
    { label: 'Acumulado', value: 'TOTAL', icon: 'pi pi-chart-line' }
  ]);

  chartTypeOptions = signal([
    { label: 'Barras', value: 'bar' },
    { label: 'Líneas', value: 'line' },
    { label: 'Sectores', value: 'pie' }
  ]);

  selectedEntity = signal<string | null>(null);
  selectedMetric = signal<string | null>(null);
  selectedProducts = signal<string[]>([]);
  selectedStores = signal<string[]>([]);
  selectedOrders = signal<string[]>([]);
  selectedUsers = signal<string[]>([]);

  entityOptions = signal<string[]>([]);
  metricOptions = signal<string[]>([]);
  productOptions = signal<string[]>([]);
  storeOptions = signal<string[]>([]);
  orderOptions = signal<string[]>([]);
  userOptions = signal<string[]>([]);

  chartType = signal<'bar' | 'line' | 'pie'>('bar');
  metricMode = signal<'VALUE' | 'TOTAL'>('VALUE');
  isLoadingReport = signal<boolean>(false);

  graphData = signal<any[]>([]);
  tableData = signal<any[]>([]);

  // SEÑALES DE PAGINACIÓN
  totalRecords = signal<number>(0);
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);

  selectedDynamicColumns = signal<string[]>([]);

  private entityFieldMap: Record<string, { id: string, name: string }> = {
    'STORE': { id: 'meta_storeId', name: 'meta_storeName' },
    'PRODUCT': { id: 'meta_productId', name: 'meta_productName' },
    'USER': { id: 'meta_userId', name: 'meta_userName' },
    'ORDER': { id: 'meta_orderId', name: 'meta_orderName' },
    'TRUCK': { id: 'meta_truckId', name: 'meta_truckName' },
    'REVIEW': { id: 'meta_reviewId', name: 'meta_reviewName' }
  };

  private getInternalEntity(): string | null {
    const selected = this.selectedEntity();
    if (!selected) return null;

    const reverseMap: Record<string, string> = {
      'Tienda': 'STORE', 'SHOP': 'STORE', 'STORE': 'STORE',
      'Producto': 'PRODUCT', 'PRODUCT': 'PRODUCT',
      'Usuario': 'USER', 'USER': 'USER',
      'Pedido': 'ORDER', 'ORDER': 'ORDER',
      'Camión': 'TRUCK', 'TRUCK': 'TRUCK',
      'Reseña': 'REVIEW', 'REVIEW': 'REVIEW'
    };
    return reverseMap[selected] || selected;
  }

  private columnTranslations: Record<string, string> = {
    'timestamp': 'Fecha',
    'metric_value': 'Variación',
    'metric_total': 'Acumulado',
    'meta_storeId': 'ID Tienda',
    'meta_storeName': 'Tienda',
    'meta_productId': 'ID Prod.',
    'meta_productName': 'Producto',
    'meta_userId': 'ID Usuario',
    'meta_userName': 'Usuario',
    'meta_orderId': 'ID Pedido',
    'meta_orderName': 'Pedido',
    'meta_truckId': 'ID Camión',
    'meta_truckName': 'Camión',
    'meta_reviewId': 'ID Reseña',
    'meta_reviewName': 'Reseña'
  };

  chartData = computed(() => {
    const rawData = this.graphData();
    if (!rawData || rawData.length === 0) return {};

    const labels = rawData.map(item => formatDate(item._id, 'dd MMM yyyy', this.locale));
    const dataValues = rawData.map(item => item.totalValue);

    const isPie = this.chartType() === 'pie';
    const bgColors = isPie
      ? ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6']
      : 'rgba(59, 130, 246, 0.5)';
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
          tension: 0.4
        }
      ]
    };
  });

  chartOptions = computed(() => {
    const isPie = this.chartType() === 'pie';
    return {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        legend: { display: isPie, position: 'right' },
        tooltip: { mode: 'index', intersect: false },

        datalabels: {
          display: true,
          align: isPie ? 'center' : 'end',
          anchor: isPie ? 'center' : 'end',
          color: isPie ? '#ffffff' : '#475569',
          font: {
            weight: 'bold',
            size: 12
          },
          formatter: (value: any) => {
            return Math.round(value * 10) / 10;
          }
        }
      },
      scales: {
        x: { display: !isPie },
        y: { display: !isPie, beginAtZero: true }
      }
    };
  });

  availableDynamicEntities = computed(() => {
    const data = this.tableData();
    const internalMain = this.getInternalEntity();
    if (data.length === 0 || !internalMain) return [];

    const foundEntities = new Set<string>();
    foundEntities.add(internalMain);

    data.forEach(row => {
      if (row.metadata) {
        if (row.metadata.storeId) foundEntities.add('STORE');
        if (row.metadata.productId) foundEntities.add('PRODUCT');
        if (row.metadata.userId) foundEntities.add('USER');
        if (row.metadata.orderId) foundEntities.add('ORDER');
        if (row.metadata.truckId) foundEntities.add('TRUCK');
        if (row.metadata.reviewId) foundEntities.add('REVIEW');
      }
    });

    return Array.from(foundEntities).map(ent => ({
      label: this.columnTranslations[`meta_${ent.toLowerCase()}Name`] || ent,
      value: ent
    }));
  });

  tableColumns = computed(() => {
    const internalMain = this.getInternalEntity();
    if (!internalMain || this.tableData().length === 0) return [];

    const cols: string[] = [];

    cols.push('timestamp');

    if (this.selectedDynamicColumns().includes(internalMain)) {
      const mainFields = this.entityFieldMap[internalMain];
      if (mainFields) {
        cols.push(mainFields.id);
        cols.push(mainFields.name);
      }
    }

    cols.push('metric_value');
    cols.push('metric_total');

    this.selectedDynamicColumns().forEach(ent => {
      if (ent !== internalMain) {
        const dynamicFields = this.entityFieldMap[ent];
        if (dynamicFields) {
          cols.push(dynamicFields.id);
          cols.push(dynamicFields.name);
        }
      }
    });

    return cols;
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
            this.storeOptions.set((referencesMap.storeId || []).filter(item => item != null));
            this.productOptions.set((referencesMap.productId || []).filter(item => item != null));
            this.userOptions.set((referencesMap.userId || []).filter(item => item != null));
            this.orderOptions.set((referencesMap.orderId || []).filter(item => item != null));
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

    const internalMain = this.getInternalEntity();
    this.selectedDynamicColumns.set(internalMain ? [internalMain] : []);
  }

  private getBaseParams() {
    const entity = this.selectedEntity();
    const metric = this.selectedMetric();
    const start = this.startDate();
    const end = this.endDate();

    if (!entity || !metric || !start || !end) return null;

    return {
      startDate: start.toISOString(),
      endDate: end.toISOString(),
      entityType: entity,
      dataType: metric,
      metricMode: this.metricMode(),
      productIds: this.selectedProducts(),
      storeIds: this.selectedStores(),
      orderIds: this.selectedOrders(),
      userIds: this.selectedUsers()
    };
  }

  fetchReportData() {
    const params = this.getBaseParams();
    if (!params) return;

    this.isLoadingReport.set(true);
    this.currentPage.set(0);

    this.registryService.loadRegistry({ ...params, viewType: 'GRAPH', interval: this.selectedInterval() } as any)
      .subscribe({
        next: (res: any) => this.graphData.set(res.items || res),
        error: () => this.graphData.set([])
      });

    this.loadTableData(params);
  }

  private loadTableData(baseParams: any) {
    this.registryService.loadRegistry({
      ...baseParams,
      viewType: 'TABLE',
      page: this.currentPage(),
      size: this.pageSize()
    } as any).subscribe({
      next: (res: any) => {
        this.tableData.set(res.items);
        this.totalRecords.set(res.totalItems);
        this.isLoadingReport.set(false);
      },
      error: () => {
        this.tableData.set([]);
        this.totalRecords.set(0);
        this.isLoadingReport.set(false);
      }
    });
  }

  onPageChange(event: any) {
    this.currentPage.set(Math.floor(event.first / event.rows));
    this.pageSize.set(event.rows);

    const params = this.getBaseParams();
    if (params) {
      this.loadTableData(params);
    }
  }

  updateMetricMode(mode: 'VALUE' | 'TOTAL') {
    if (this.metricMode() !== mode) {
      this.metricMode.set(mode);
      this.fetchReportData();
    }
  }

  getColumnLabel(col: string): string {
    return this.columnTranslations[col] || col.replace('meta_', '').replace('metric_', '');
  }

  getCellValue(row: any, column: string): any {
    if (column === 'timestamp') return row.timestamp;

    if (column.startsWith('metric_')) {
      const metricKey = column.replace('metric_', '');
      return row.metrics?.[metricKey] ?? '-';
    }

    if (column.startsWith('meta_')) {
      const metaKey = column.replace('meta_', '');
      return row.metadata?.[metaKey] ?? '-';
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
    this.totalRecords.set(0);
  }

  downloadCustomPdf() {
    const params = this.getBaseParams();
    if (!params) return;

    this.isLoadingReport.set(true);

    const base64Chart = this.chartComponent.chart.toBase64Image();

    const currentIntervalValue = this.selectedInterval();
    const intervalObj = this.intervalOptions().find(opt => opt.value === currentIntervalValue);
    const intervalLabel = intervalObj ? intervalObj.label : 'General';

    const payload = {
      ...params,
      interval: intervalLabel,
      chartImage: base64Chart
    };

    this.registryService.exportCustomPdf(payload).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Informe_${this.selectedEntity()}_Personalizado.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.isLoadingReport.set(false);
      },
      error: () => {
        console.error('Error generando PDF');
        this.isLoadingReport.set(false);
      }
    });
  }
}
