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
import { StatService } from '../../../services/stat.service';
import {formatAddress, formatPrice} from '../../../utils/textFormat.util';
import { LoginInfo } from '../../../models/loginInfo.model';
import {OrderService} from '../../../services/order.service';
import {Order} from '../../../models/order.model';
import {catchError, forkJoin, of, switchMap} from 'rxjs';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {StyleClass} from 'primeng/styleclass';

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
    Button, ChartModule, TableModule, Tag, Avatar, Tooltip, StyleClass
  ],
  templateUrl: './admin-home.component.html'
})
export class AdminHomeComponent implements OnInit {

  loading = true;
  error = false;
  currentDate = new Date();

  driverTruck = signal<any | null>(null);
  driverShop = signal<any | null>(null);

  globalKpis = signal({ totalBudget: 0, activeOrders: 0, totalOrders:0, activeTrucks: 0, totalTrucks: 0, totalShops: 0 });

  driverKpis = signal({
    orderMade: 0,
    sent: 0,
    onDelivery: 0,
    completed: 0,
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

  private metricService = inject(StatService);

  constructor(protected authService: AuthService,
              private orderService: OrderService,
              private shopService: ShopService,
              private truckService: TruckService) {}

  ngOnInit() {
    this.getLoginInfo();
    this.initChartOptions();
  }

  getLoginInfo() {
    this.loading = true;
    this.authService.getLoginInfo().subscribe({
      next: (info) => {
        this.loginInfo = info;
        this.initData();
      },
      error: (err) => {
        console.error('Error obteniendo la información de sesión', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  initData() {
    this.loading = true;
    this.error = false;

    if (this.authService.isAdmin() || this.authService.isManager()) {
      this.loadKpis();
      this.loadSalesChartMock();
      this.loadRecentOrdersByRole();
      this.loadSystemAlertsMock();
    } else if (this.authService.isDriver()) {
      this.loadContactInformation();
    }
  }

  private loadKpis() {
    this.loading = true;
    this.error = false;

    if (this.authService.isAdmin() || this.authService.isManager()) {

      forkJoin({
        orders: this.metricService.getOrdersStatsByRole(),
        shops: this.metricService.getShopsStatsByRole(),
        trucks: this.metricService.getTrucksStatsByRole()
      }).subscribe({
        next: ({ orders, shops, trucks }) => {
          // Shop stats
          const totalBudget = Number(shops.find(m => m.label === 'Presupuesto Total')?.value || 0);
          const totalShops = Number(shops.find(m => m.label === 'Tiendas')?.value || 0);

          // Order stats
          const orderMade = Number(orders.find(m => m.label === 'Realizados')?.value || 0);
          const sent = Number(orders.find(m => m.label === 'Enviados')?.value || 0);
          const onDelivery = Number(orders.find(m => m.label === 'En Reparto')?.value || 0);
          const completed = Number(orders.find(m => m.label === 'Completados')?.value || 0);

          //Truck stats
          const available = Number(trucks.find(m => m.label === 'Disponibles')?.value || 0);
          const onRoute = Number(trucks.find(m => m.label === 'En Ruta')?.value || 0);
          const onMaintenance = Number(trucks.find(m => m.label === 'En mantenimiento')?.value || 0);
          const outOfService = Number(trucks.find(m => m.label === 'Fuera de servicio')?.value || 0);


          const activeOrders = orderMade + sent + onDelivery;
          const activeTrucks = available + onRoute;


          this.globalKpis.set({
            totalBudget: totalBudget,
            activeOrders: activeOrders,
            totalOrders: activeOrders + completed,
            activeTrucks: activeTrucks,
            totalTrucks: activeTrucks + onMaintenance + outOfService,
            totalShops: totalShops
          });

          this.ordersChartData.set({
            labels: ['Completados', 'En Reparto', 'Enviados', 'Realizados'],
            datasets: [{
              data: [completed, onDelivery, sent, orderMade],
              backgroundColor: ['#22c55e', '#3b82f6', '#f59e0b', '#64748b'],
              hoverOffset: 4,
              borderWidth: 0
            }]
          });

          this.loading = false;
        },
        error: (err) => {
          console.error('Error cargando KPIs globales:', err);
          this.error = true;
          this.loading = false;
        }
      });

    } else if (this.authService.isDriver()) {

      this.metricService.getOrdersStatsByRole().subscribe({
        next: (orders) => {
          const completed = Number(orders.find(m => m.label === 'Completados')?.value || 0);
          const orderMade = Number(orders.find(m => m.label === 'Realizados')?.value || 0);
          const sent = Number(orders.find(m => m.label === 'Enviados')?.value || 0);
          const onDelivery = Number(orders.find(m => m.label === 'En Reparto')?.value || 0);

          // 2. Actualizamos los KPIs del conductor con los valores reales
          this.driverKpis.update(current => ({
            ...current,
            orderMade,
            sent,
            onDelivery,
            completed
          }));

          this.loading = false;
        },
        error: (err) => {
          console.error('Error cargando KPIs del conductor:', err);
          this.error = true;
          this.loading = false;
        }
      });
    }
  }

  private loadContactInformation() {
    if (!this.loginInfo || !this.loginInfo.id) return;

    this.truckService.getAssignedTruckByDriverId(this.loginInfo.id).pipe(
      switchMap(truck => {
        // Scenario 1: No truck
        if (!truck) {
          this.driverTruck.set(null);
          return of(null); // Stop execution
        }

        // Save the truck and request the shop
        this.driverTruck.set(truck);
        return this.shopService.getShopByAssignedTruckId(truck.id).pipe(
          catchError(() => of(null)) // If shop response fails, the loading continues (scenario 2)
        );
      }),
      catchError(err => {
        console.error('Error cargando información operativa:', err);
        this.driverTruck.set(null);
        return of(null);
      })
    ).subscribe({
      next: (shop) => {
        // Scenario 1: No truck
        if (!this.driverTruck()) {
          this.loading = false; //Stop loading so the warning message can be displayed
          return;
        }

        this.driverShop.set(shop || null);

        // Scenario 2: Truck, but no shop
        if (!shop) {
          this.loadSystemAlertsMock();
          this.loading = false;
        }
        // Scenario 3: Truck and Shop
        else {
          this.loadDriverData();
        }
      }
    });
  }

  private loadDriverData() {
    this.metricService.getOrdersStatsByRole().subscribe({
      next: (orders) => {
        this.driverKpis.set({
          orderMade: Number(orders.find(m => m.label === 'Realizados')?.value || 0),
          sent: Number(orders.find(m => m.label === 'Enviados')?.value || 0),
          onDelivery: Number(orders.find(m => m.label === 'En Reparto')?.value || 0),
          completed: Number(orders.find(m => m.label === 'Completados')?.value || 0)
        });

        this.loadDriverHistoryMock();
        this.loadRecentOrdersByRole();
        this.loadSystemAlertsMock();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error cargando KPIs del conductor:', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  // Sales chart (mock)
  private loadSalesChartMock() {
    this.salesChartData.set({
      labels: ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'],
      datasets: [{
        label: 'Ingresos',
        data: [2500, 3200, 2800, 4100, 3900, 5200, 2880.50],
        fill: true,
        borderColor: '#3b82f6',
        tension: 0.4,
        backgroundColor: 'rgba(59, 130, 246, 0.1)'
      }]
    });
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

  // 3. Modificado para generar datos de barras apiladas separados en 4 estados
  private loadDriverHistoryMock() {
    this.driverHistoryChartData.set({
      labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'],
      datasets: [
        {
          label: 'Completados',
          data: [15, 18, 14, 20, 19, 22, 12],
          backgroundColor: '#22c55e'
        },
        {
          label: 'En Reparto',
          data: [2, 1, 3, 0, 2, 1, 0],
          backgroundColor: '#3b82f6'
        },
        {
          label: 'Enviados',
          data: [1, 2, 1, 1, 0, 2, 1],
          backgroundColor: '#f59e0b'
        },
        {
          label: 'Realizados',
          data: [0, 1, 0, 2, 1, 0, 0],
          backgroundColor: '#64748b'
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

    // 4. Modificado para permitir 'stacked' (apilamiento) en X e Y
    this.driverHistoryChartOptions.set({
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, color: textColor, padding: 20 } },
        tooltip: { mode: 'index', intersect: false }
      },
      scales: {
        x: { stacked: true, ticks: { color: textColorSecondary }, grid: { display: false, drawBorder: false } },
        y: { stacked: true, ticks: { color: textColorSecondary }, grid: { color: surfaceBorder, drawBorder: false } }
      }
    });

    this.ordersChartOptions.set({
      maintainAspectRatio: false,
      layout: { padding: 1 },
      plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, color: textColor, font: { weight: 'bold' }, padding: 20 } } },
      cutout: '65%'
    });
  }

  getAlertColors(severity: string) {
    if (severity === 'high') return 'bg-red-50 text-red-500 border-red-100';
    if (severity === 'medium') return 'bg-orange-50 text-orange-500 border-orange-100';
    return 'bg-blue-50 text-blue-500 border-blue-100';
  }

  protected readonly formatPrice = formatPrice;
  protected readonly formatAddress = formatAddress;
}
