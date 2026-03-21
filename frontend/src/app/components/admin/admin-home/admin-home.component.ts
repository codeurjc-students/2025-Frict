import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Button } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Avatar } from 'primeng/avatar';
import { Tooltip } from 'primeng/tooltip';

import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { AuthService } from '../../../services/auth.service';
import { MetricService } from '../../../services/metric.service';
import {formatAddress, formatPrice} from '../../../utils/textFormat.util';
import { LoginInfo } from '../../../models/loginInfo.model';
import {OrderService} from '../../../services/order.service';
import {Order} from '../../../models/order.model';

interface SystemAlert {
  title: string;
  description: string;
  severity: 'high' | 'medium' | 'info';
  icon: string;
  entity: 'product' | 'truck' | 'shop' | 'order' | 'route';
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink, LoadingScreenComponent,
    Button, ChartModule, TableModule, Tag, Avatar, Tooltip
  ],
  templateUrl: './admin-home.component.html'
})
export class AdminHomeComponent implements OnInit {

  loading = true;
  error = false;
  currentDate = new Date();

  globalKpis = signal({
    totalBudget: 0,
    activeOrders: 0,
    activeTrucks: 0,
    totalShops: 0
  });

  driverKpis = signal({
    assignedToday: 0,
    delivered: 0,
    pending: 0,
    truckReference: 'TRK-001', // Mock data
    truckStatus: 'En Ruta',
    shopName: 'Madrid - Recoletos',
    shopPhone: '+34 912 345 678'
  });

  salesChartData = signal<any>({});
  salesChartOptions = signal<any>({});
  driverHistoryChartOptions = signal<any>({});

  ordersChartData = signal<any>({});
  ordersChartOptions = signal<any>({});

  driverHistoryChartData = signal<any>({});

  recentOrders = signal<Order[]>([]);
  systemAlerts = signal<SystemAlert[]>([]);

  loginInfo!: LoginInfo;

  private metricService = inject(MetricService);

  constructor(public authService: AuthService,
              public orderService: OrderService) {}

  ngOnInit() {
    this.getLoginInfo();
    this.initChartOptions();
    this.initData();
  }

  getLoginInfo(){
    this.authService.getLoginInfo().subscribe({
      next: (info) => {
        this.loginInfo = info;
      }
    });
  }

  initData() {
    this.loading = true;
    this.error = false;

    this.loadKpis();

    // Separación de lógica por roles para datos específicos
    if (this.authService.isAdmin() || this.authService.isManager()) {
      this.loadSalesChartMock();
      this.loadOrdersDistributionMock();
    } else if (this.authService.isDriver()) {
      this.loadDriverHistoryMock(); // Nueva función
    }

    this.loadRecentOrdersByRole();
    this.loadSystemAlertsMock();
  }

  private loadKpis() {
    this.metricService.getDashboardStats().subscribe({
      next: (metrics) => {
        if (this.authService.isAdmin() || this.authService.isManager()) {
          this.globalKpis.set({
            totalBudget: Number(metrics.find(m => m.label === 'Presupuesto Total')?.value || 0),
            activeOrders: Number(metrics.find(m => m.label === 'Pedidos Activos')?.value || 0),
            activeTrucks: Number(metrics.find(m => m.label === 'Camiones Operativos')?.value || 0),
            totalShops: Number(metrics.find(m => m.label === 'Tiendas')?.value || 0)
          });
        } else if (this.authService.isDriver()) {
          this.driverKpis.update(current => ({
            ...current,
            assignedToday: Number(metrics.find(m => m.label === 'Total Asignados')?.value || 0),
            delivered: Number(metrics.find(m => m.label === 'Completados')?.value || 0),
            pending: Number(metrics.find(m => m.label === 'Pendientes')?.value || 0)
          }));
        }
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  // Sales chart (mock)
  private loadSalesChartMock() {
    this.salesChartData.set({
      labels: ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'],
      datasets: [{ label: 'Ingresos', data: [2500, 3200, 2800, 4100, 3900, 5200, 2880.50], fill: true, borderColor: '#3b82f6', tension: 0.4, backgroundColor: 'rgba(59, 130, 246, 0.1)' }]
    });
  }

  // Distribution shart (mock)
  private loadOrdersDistributionMock() {
    if (this.authService.isAdmin() || this.authService.isManager()) {
      this.ordersChartData.set({
        labels: ['Completados', 'En Reparto', 'Pendientes', 'Cancelados'],
        datasets: [{ data: [350, 85, 42, 15], backgroundColor: ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444'], hoverOffset: 4, borderWidth: 0 }]
      });
    } else {
      this.ordersChartData.set({
        labels: ['Entregados', 'Pendientes'],
        // Usamos los datos dinámicos aunque estemos en el mock (si ya llegaron)
        datasets: [{ data: [this.driverKpis().delivered || 28, this.driverKpis().pending || 17], backgroundColor: ['#22c55e', '#f59e0b'], hoverOffset: 4, borderWidth: 0 }]
      });
    }
  }


  private loadRecentOrdersByRole() {
    this.orderService.getOrdersByRolePage(0, 5, 'createdAt,desc').subscribe({
      next: (response) => {
        console.log(response);
        this.recentOrders.set(response.items);
      },
      error: (err) => {
        console.error('Error cargando pedidos recientes', err);
      }
    });
  }

  // Alerts table (mock)
  private loadSystemAlertsMock() {
    if (this.authService.isAdmin() || this.authService.isManager()) {
      this.systemAlerts.set([
        { title: 'Stock Crítico', description: 'El producto "Caja de Herramientas V2" tiene menos de 5 unidades.', severity: 'high', icon: 'pi pi-box', entity: 'product' },
        { title: 'Retraso de Flota', description: 'El camión TRK-003 reporta un retraso de 45 min en su ruta actual.', severity: 'medium', icon: 'pi pi-truck', entity: 'truck' },
        { title: 'Pico de Pedidos', description: 'La tienda ha recibido 50 pedidos en la última hora.', severity: 'info', icon: 'pi pi-shop', entity: 'shop' }
      ]);
    } else {
      this.systemAlerts.set([
        { title: 'Tráfico Denso', description: 'Retención de 15 mins en la M-30 dirección Sur.', severity: 'medium', icon: 'pi pi-map-marker', entity: 'route' },
        { title: 'Revisión Programada', description: 'Recuerda llevar el vehículo al taller al finalizar la jornada.', severity: 'info', icon: 'pi pi-wrench', entity: 'truck' }
      ]);
    }
  }

  private loadDriverHistoryMock() {
    this.driverHistoryChartData.set({
      labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'],
      datasets: [
        {
          label: 'Completados',
          data: [12, 19, 15, 22, 18, 25, 14],
          borderColor: '#22c55e',
          backgroundColor: 'rgba(34, 197, 94, 0.1)',
          fill: true,
          tension: 0.4
        },
        {
          label: 'Pendientes/Incidencias',
          data: [2, 4, 1, 5, 2, 3, 2],
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.1)',
          fill: true,
          tension: 0.4
        }
      ]
    });
  }

  private initChartOptions() {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color') || '#475569';
    const textColorSecondary = documentStyle.getPropertyValue('--text-color-secondary') || '#64748b';
    const surfaceBorder = documentStyle.getPropertyValue('--surface-border') || '#e2e8f0';

    this.salesChartOptions.set({
      maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: (context: any) => new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(context.parsed.y) } } },
      scales: { x: { ticks: { color: textColorSecondary }, grid: { color: surfaceBorder, drawBorder: false } }, y: { ticks: { color: textColorSecondary }, grid: { color: surfaceBorder, drawBorder: false } } }
    });

    this.driverHistoryChartOptions.set({
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { x: { ticks: { color: textColorSecondary }, grid: { color: surfaceBorder, drawBorder: false } }, y: { ticks: { color: textColorSecondary }, grid: { color: surfaceBorder, drawBorder: false } } }
    });

    this.ordersChartOptions.set({
      maintainAspectRatio: false,
      layout: { padding: 1 },
      plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, color: textColor, font: { weight: 'bold' }, padding: 20 } } },
      cutout: '65%'
    });
  }

  getOrderStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    const map: Record<string, any> = { 'Completado': 'success', 'En Reparto': 'info', 'Enviado': 'info', 'Pedido Realizado': 'warn', 'Cancelado': 'danger' };
    return map[status] || 'secondary';
  }

  getAlertColors(severity: string) {
    if (severity === 'high') return 'bg-red-50 text-red-500 border-red-100';
    if (severity === 'medium') return 'bg-orange-50 text-orange-500 border-orange-100';
    return 'bg-blue-50 text-blue-500 border-blue-100';
  }

  protected readonly formatPrice = formatPrice;
  protected readonly formatAddress = formatAddress;
}
