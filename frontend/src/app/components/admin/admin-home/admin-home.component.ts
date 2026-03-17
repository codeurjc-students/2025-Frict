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
import { formatPrice } from '../../../utils/textFormat.util';
import { LoginInfo } from '../../../models/loginInfo.model';

interface RecentOrder {
  id: string;
  reference: string;
  customer: string;
  total: number;
  status: string;
  date: string;
  address?: string;
}

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

  ordersChartData = signal<any>({});
  ordersChartOptions = signal<any>({});

  recentOrders = signal<RecentOrder[]>([]);
  systemAlerts = signal<SystemAlert[]>([]);

  loginInfo!: LoginInfo;

  private metricService = inject(MetricService);

  constructor(public authService: AuthService) {}

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

    this.loadKpis(); // Real
    this.loadSalesChartMock(); // Mock
    this.loadOrdersDistributionMock(); // Mock
    this.loadRecentOrdersMock(); // Mock
    this.loadSystemAlertsMock(); // Mock
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

  // Recent orders table (mock)
  private loadRecentOrdersMock() {
    if (this.authService.isAdmin() || this.authService.isManager()) {
      this.recentOrders.set([
        { id: '1023', reference: 'ORD-A1B2', customer: 'Carlos Ruiz', total: 125.50, status: 'En Reparto', date: new Date().toISOString() },
        { id: '1024', reference: 'ORD-X9Y8', customer: 'Ana Gómez', total: 45.00, status: 'Pedido Realizado', date: new Date().toISOString() },
        { id: '1025', reference: 'ORD-M4N5', customer: 'Luis Pérez', total: 310.20, status: 'Enviado', date: new Date().toISOString() },
        { id: '1026', reference: 'ORD-P7Q6', customer: 'Marta López', total: 89.99, status: 'Completado', date: new Date().toISOString() },
        { id: '1027', reference: 'ORD-PQ48', customer: 'Sandra Isidro', total: 109.99, status: 'Enviado', date: new Date().toISOString() }
      ]);
    } else {
      this.recentOrders.set([
        { id: '2001', reference: 'ORD-A1B2', customer: 'Carlos Ruiz', total: 0, status: 'En Reparto', address: 'Calle Mayor 12, Madrid', date: new Date().toISOString() },
        { id: '2002', reference: 'ORD-C3D4', customer: 'Marta López', total: 0, status: 'En Reparto', address: 'Paseo del Prado 45, Madrid', date: new Date().toISOString() },
        { id: '2003', reference: 'ORD-E5F6', customer: 'Juan Gómez', total: 0, status: 'Completado', address: 'Calle Alcalá 102, Madrid', date: new Date().toISOString() }
      ]);
    }
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
}
