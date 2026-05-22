import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {Router, RouterLink} from '@angular/router';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Paginator} from 'primeng/paginator';
import {InputText} from 'primeng/inputtext';

import * as L from 'leaflet';
import {Tooltip} from 'primeng/tooltip';
import {Shop} from '../../../models/shop.model';
import {ShopService} from '../../../services/shop.service';
import {PageResponse} from '../../../models/pageResponse.model';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {formatAddress} from '../../../utils/textFormat.util';
import {MessageService} from 'primeng/api';
import {Dialog} from 'primeng/dialog';
import {User} from '../../../models/user.model';
import {Select} from 'primeng/select';
import {UserService} from '../../../services/user.service';
import {Avatar} from 'primeng/avatar';
import {AuthService} from '../../../services/auth.service';
import {Notification} from '../../../models/notification.model';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {UiService} from '../../../utils/ui.service';
import {NotificationService} from '../../../services/notification.service';

@Component({
  selector: 'app-shops-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    RouterLink, InputGroup, InputGroupAddon, Button, TableModule, Paginator, InputText, Tooltip, LoadingScreenComponent, Dialog, Select, Avatar, BreadcrumbReloadComponent
  ],
  templateUrl: './shops-management.component.html',
  styleUrl: 'shops-management.component.css'
})
export class ShopsManagementComponent implements OnInit, OnDestroy {

  private shopService = inject(ShopService);
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  protected authService = inject(AuthService);
  protected uiService = inject(UiService);
  protected notificationService = inject(NotificationService);
  private router = inject(Router);

  recentShopsNotifications = signal<Notification[]>([]);

  // Pagination
  shopsPage: PageResponse<Shop> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  first = 0;
  rows = 10;

  // Leaflet map
  private map: L.Map | undefined;
  private markers: L.Marker[] = [];

  // Loading
  protected loading: boolean = true;
  protected error: boolean = false;

  // Assignment dialog
  protected currentShop: Shop | undefined = undefined;
  protected managers: User[] = [];
  protected selectedManager: User | undefined = undefined;
  protected visibleAssignmentDialog: boolean = false;
  protected visibleUnassignButton: boolean = false;

  ngOnInit() {
    this.loadShops();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  // Leaflet map (visualization only)
  private initMap(): void {
    this.map = L.map('map', {
      zoomControl: false // Zoom control on top left side
    }).setView([40.4168, -3.7038], 6);

    L.control.zoom({ position: 'topright' }).addTo(this.map);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(this.map);
    this.map.attributionControl.setPrefix('Leaflet');

    this.renderShopMarkers();
  }

  private renderShopMarkers() {
    if (!this.map) return;

    this.markers.forEach(marker => this.map!.removeLayer(marker));
    this.markers = [];

    const shopIcon = L.icon({
      iconUrl: './location-pointer.png',
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -28]
    });

    this.shopsPage.items.forEach(shop => {
      if (shop.address.latitude && shop.address.longitude) {
        const marker = L.marker([shop.address.latitude, shop.address.longitude], { icon: shopIcon })
          .addTo(this.map!)
          .bindPopup(`
            <div class="p-2 min-w-[140px] text-center">
                <h4 class="font-bold text-slate-800 text-sm">Tienda ${shop.name}</h4>
                <p class="text-xs text-slate-500 mb-2">${formatAddress(shop.address)}</p>
                <span class="bg-cyan-100 text-cyan-800 text-[10px] px-2 py-0.5 rounded-full font-bold">
                    Stock: ${shop.totalAvailableProducts} productos
                </span>
            </div>
        `);
        this.markers.push(marker);
      }
    });
  }

  public reloadAll() {
    this.loading = true;
    this.error = false;

    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
    this.cancelAssignment();
    this.loadShops();
  }

  loadShops() {
    let request$;
    if (this.authService.isAdmin()) {
      request$ = this.shopService.getAllShopsPage(this.first / this.rows, this.rows);
    } else {
      request$ = this.shopService.getAssignedShopsPage(this.first / this.rows, this.rows);
    }

    request$.subscribe({
      next: (shops) => {
        this.shopsPage = shops;
        this.loading = false;

        setTimeout(() => {
          if (!this.map) {
            this.initMap();
          } else {
            this.renderShopMarkers();
          }
        }, 10);

        this.getShopRecentNotifications();
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  getShopRecentNotifications(){
    this.notificationService.getRecentNotifications('SHOP', 3).subscribe({
      next: (list) => {
        this.recentShopsNotifications.set(list);
      }
    })
  }

  //Fly to selected shop in map
  locateShopOnMap(shop: Shop) {
    if (this.map && shop.address.latitude && shop.address.longitude) {
      this.map.flyTo([shop.address.latitude, shop.address.longitude], 14, {
        duration: 1.5 //Seconds
      });
    }
  }

  deleteShop(id: string) {
    this.shopService.deleteShop(id).subscribe({
      next: (shop) => {
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Tienda ${shop.name} borrada correctamente.` });
        this.loadShops();
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: `No se ha podido borrar la tienda.` });
      }
    })
  }

  onPageChange(event: any) {
    this.first = event.first;
    this.rows = event.rows;
    this.loadShops();
  }

  showAssignmentDialog(shopId: string) {
    this.userService.getAllUsersByRole("MANAGER").subscribe({
      next: (managers) => {
        this.managers = managers;
        const foundShop = this.shopsPage.items.find(shop => shop.id === shopId);

        if (foundShop) {
          this.currentShop = foundShop;
          this.selectedManager = foundShop.assignedManager;
          this.visibleUnassignButton = !!this.selectedManager;
        }
        this.visibleAssignmentDialog = true;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los gerentes' });
      }
    });
  }

  cancelAssignment(){
    this.selectedManager = undefined;
    this.currentShop = undefined;
    this.visibleAssignmentDialog = false;
    this.visibleUnassignButton = false;
  }

  setManagerAssignment(userId: string | undefined, state: boolean){
    const managerId = userId;
    const shopId = this.currentShop?.id;
    if (managerId && shopId){
      this.shopService.assignManager(shopId, managerId, state).subscribe({
        next: (shop) => {
          const index = this.shopsPage.items.findIndex(item => item.id == shop.id);
          if (index !== -1) {
            this.shopsPage.items[index] = shop;
          }
          this.cancelAssignment();
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: `No se ha podido completar la asignación del gerente a la tienda.` });
        }
      })
    }
  }

  goToNotification(notif: Notification) {
    this.router.navigate(['/admin/notifications'], {
      queryParams: { notifId: notif.id },
      state: { notification: notif }
    });
  }

  protected readonly formatAddress = formatAddress;
}
