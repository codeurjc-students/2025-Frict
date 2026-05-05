import {Component, inject, LOCALE_ID, OnInit, signal} from '@angular/core';
import {CommonModule, formatDate} from '@angular/common';
import {RouterLink} from '@angular/router';

import {Button} from 'primeng/button';
import {ChartModule} from 'primeng/chart';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Avatar} from 'primeng/avatar';
import {Tooltip} from 'primeng/tooltip';
import {Notification} from '../../../models/notification.model'

import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {AuthService} from '../../../services/auth.service';
import {StatService} from '../../../services/stat.service';
import {formatAddress, formatPrice} from '../../../utils/textFormat.util';
import {LoginInfo} from '../../../models/loginInfo.model';
import {OrderService} from '../../../services/order.service';
import {Order} from '../../../models/order.model';
import {catchError, forkJoin, map, of, switchMap} from 'rxjs';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {StyleClass} from 'primeng/styleclass';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {NotificationService} from '../../../services/notification.service';
import {UiService} from '../../../utils/ui.service';
import {RegistryService} from '../../../services/registry.service'; // <-- NUEVO IMPORT

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink, LoadingScreenComponent,
    Button, ChartModule, TableModule, Tag, Avatar, Tooltip, StyleClass, BreadcrumbReloadComponent
  ],
  templateUrl: './admin-home.component.html'
})
export class AdminHomeComponent implements OnInit {

  loading = true;
  loadingNotifications = true;
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
  recentNotifications = signal<Notification[]>([]);

  loginInfo!: LoginInfo;

  private metricService = inject(StatService);
  private registryService = inject(RegistryService); // <-- NUEVA INYECCIÓN
  private locale = inject(LOCALE_ID); // <-- NUEVA INYECCIÓN

  constructor(protected authService: AuthService,
              private orderService: OrderService,
              private shopService: ShopService,
              private truckService: TruckService,
              private notificationService: NotificationService,
              protected uiService: UiService) {}

  ngOnInit() {
    this.getLoginInfo();
    this.initChartOptions();
  }

  getLoginInfo() {
    this.loading = true;
    this.loadingNotifications = true;
    this.authService.getLoginInfo().subscribe({
      next: (info) => {
        this.loginInfo = info;
        this.initData();
      },
      error: (err) => {
        console.error('Error obtaining session info: ', err);
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
      this.loadSalesChartData();
      this.loadRecentOrdersByRole();
      this.loadRecentNotifications();
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
          this.loadRecentNotifications();
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

        this.loadDriverHistoryData();
        this.loadRecentOrdersByRole();
        this.loadRecentNotifications();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error cargando KPIs del conductor:', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  private loadSalesChartData() {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - 6);

    const baseParams: any = {
      startDate: startDate.toISOString(),
      endDate: endDate.toISOString(),
      entityType: 'SHOP',
      dataType: 'SHOP_BUDGET',
      metricMode: 'VALUE',
      viewType: 'GRAPH',
      interval: 'day'
    };

    const lineColors = ['#3b82f6', '#22c55e', '#f59e0b', '#a855f7', '#ef4444', '#06b6d4', '#ec4899'];

    if (this.authService.isManager()) {

      this.shopService.getManagedShopReferences().pipe(
        switchMap(shops => {
          if (!shops || shops.length === 0) return of([]);

          const requests = shops.map(shop => {
            const shopParams = { ...baseParams, shopIds: [shop.referenceCode] };
            return this.registryService.loadInternalRegistry(shopParams).pipe(
              map((res: any) => ({
                name: shop.name, // Guardamos el nombre para la leyenda
                data: res.items || res
              }))
            );
          });

          return forkJoin(requests);
        })
      ).subscribe({
        next: (results: any[]) => {
          console.log(results);

          if (results.length === 0) return;

          const labels = results[0].data.map((item: any) => formatDate(item._id, 'dd MMM', this.locale));

          const datasets = results.map((result, index) => {
            const dataValues = result.data.map((item: any) => item.totalValue);
            const color = lineColors[index % lineColors.length];

            return {
              label: result.name,
              data: dataValues,
              fill: false,
              borderColor: color,
              backgroundColor: color,
              tension: 0.4,
              borderWidth: 2
            };
          });

          this.salesChartData.set({ labels, datasets });

          const currentOptions = this.salesChartOptions();
          this.salesChartOptions.set({
            ...currentOptions,
            plugins: {
              ...currentOptions.plugins,
              legend: { display: true, position: 'bottom', labels: { usePointStyle: true } }
            }
          });
        },
        error: (err) => console.error('Error cargando gráfica multilínea', err)
      });

    } else {
      this.registryService.loadInternalRegistry(baseParams).subscribe({
        next: (res: any) => {
          const rawData = res.items || res;
          const labels = rawData.map((item: any) => formatDate(item._id, 'dd MMM', this.locale));
          const dataValues = rawData.map((item: any) => item.totalValue);

          this.salesChartData.set({
            labels: labels,
            datasets: [{
              label: 'Presupuesto Global',
              data: dataValues,
              fill: true,
              borderColor: '#3b82f6',
              tension: 0.4,
              backgroundColor: 'rgba(59, 130, 246, 0.1)'
            }]
          });
        },
        error: (err) => console.error('Error cargando gráfica global', err)
      });
    }
  }

  private loadRecentOrdersByRole() {
    this.orderService.getOrdersByRolePage(0, 5, 'createdAt,desc').subscribe({
      next: (response) => {
        this.recentOrders.set(response.items);
      },
      error: (err) => {
        console.error('Error cargando pedidos recientes', err);
      }
    });
  }

  private loadRecentNotifications() {
    this.loadingNotifications = true;
    this.notificationService.getRecentNotifications('', 3).subscribe({
      next: (notifications) => {
        this.recentNotifications.set(notifications);
        this.loadingNotifications = false;
      },
      error: () => {
        this.loadingNotifications = false;
      }
    });
  }


  private loadDriverHistoryData() {
    const truck = this.driverTruck();
    if (!truck) return; // Si el conductor no tiene camión, no pedimos la gráfica

    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - 6);

    const baseParams: any = {
      startDate: startDate.toISOString(),
      endDate: endDate.toISOString(),
      entityType: 'ORDER',
      metricMode: 'VALUE', // "Modo variación" (eventos diarios)
      viewType: 'GRAPH',
      interval: 'day',
      userIds: [this.loginInfo.username]
    };

    // Preparamos las dos peticiones
    const reqCompleted = this.registryService.loadInternalRegistry({ ...baseParams, dataType: 'ORDERS_COMPLETED' });
    const reqCancelled = this.registryService.loadInternalRegistry({ ...baseParams, dataType: 'ORDERS_CANCELLED' });

    // Las lanzamos en paralelo
    forkJoin([reqCompleted, reqCancelled]).subscribe({
      next: ([resCompleted, resCancelled]: [any, any]) => {
        const dataCompleted = resCompleted.items || resCompleted;
        const dataCancelled = resCancelled.items || resCancelled;

        const completedToday = dataCompleted.length > 0 ? dataCompleted[dataCompleted.length - 1].totalValue : 0;
        
        this.driverKpis.update(current => ({
          ...current,
          completed: completedToday
        }));

        const labels = dataCompleted.map((item: any) => formatDate(item._id, 'dd MMM', this.locale));

        this.driverHistoryChartData.set({
          labels: labels,
          datasets: [
            {
              type: 'line',
              label: 'Completados',
              data: dataCompleted.map((item: any) => item.totalValue),
              fill: false,
              borderColor: '#22c55e',
              backgroundColor: '#22c55e',
              tension: 0.4,
              borderWidth: 2
            },
            {
              type: 'line',
              label: 'Cancelados',
              data: dataCancelled.map((item: any) => item.totalValue),
              fill: false,
              borderColor: '#ef4444',
              backgroundColor: '#ef4444',
              tension: 0.4,
              borderWidth: 2
            }
          ]
        });
      },
      error: (err) => console.error('Error cargando el historial del conductor', err)
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
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, color: textColor, padding: 20 } },
        tooltip: { mode: 'index', intersect: false }
      },
      scales: {
        x: { ticks: { color: textColorSecondary }, grid: { display: false, drawBorder: false } }, // <-- Quitamos stacked: true
        y: { ticks: { color: textColorSecondary }, grid: { color: surfaceBorder, drawBorder: false } } // <-- Quitamos stacked: true
      }
    });

    this.ordersChartOptions.set({
      maintainAspectRatio: false,
      layout: { padding: 1 },
      plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, color: textColor, font: { weight: 'bold' }, padding: 20 } } },
      cutout: '65%'
    });
  }

  protected readonly formatPrice = formatPrice;
  protected readonly formatAddress = formatAddress;
}
