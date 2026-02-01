import { Component, OnInit, signal, AfterViewInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {RouterLink} from '@angular/router';
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

interface ShopAlert {
  shopName: string;
  message: string;
  severity: 'high' | 'medium' | 'info';
  icon: string;
}

@Component({
  selector: 'app-shops-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    RouterLink, InputGroup, InputGroupAddon, Button, TableModule, Paginator, InputText, Tooltip, LoadingScreenComponent, Dialog, Select, Avatar
  ],
  templateUrl: './shops-management.component.html',
  styleUrl: 'shops-management.component.css'
})
export class ShopsManagementComponent implements OnInit, OnDestroy {

  shopAlerts = signal<ShopAlert[]>([
    {
      shopName: 'Tienda de Ejemplo 1',
      message: 'Stock crítico (-15%)',
      severity: 'high',
      icon: 'pi pi-exclamation-triangle'
    },
    {
      shopName: 'Tienda de Ejemplo 2',
      message: 'Nuevo camión asignado',
      severity: 'info',
      icon: 'pi pi-truck'
    },
    {
      shopName: 'Tienda de Ejemplo 3',
      message: 'Retraso en entrega OR-442-Y5O3',
      severity: 'medium',
      icon: 'pi pi-clock'
    },
    {
      shopName: 'Tienda de Ejemplo 4',
      message: 'Inventario completado',
      severity: 'info',
      icon: 'pi pi-check-circle'
    }
  ]);

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
  protected managers: User[] = [];
  protected selectedManager: User | undefined = undefined;
  protected visibleAssignmentDialog: boolean = false;
  protected visibleUnassignButton: boolean = false;

  constructor(private shopService: ShopService,
              private userService: UserService,
              private messageService: MessageService) {}

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

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap',
      maxZoom: 19
    }).addTo(this.map);

    this.renderShopMarkers();
  }

  private renderShopMarkers() {
    if (!this.map) return;

    // Custom shops icon
    const shopIcon = L.icon({
      iconUrl: './location-pointer.png',
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -28]
    });

    this.shopsPage.items.forEach(shop => {
      const marker = L.marker([shop.latitude, shop.longitude], { icon: shopIcon })
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
    });
  }

  loadShops(){
    this.shopService.getShopsPage(this.first/this.rows, this.rows).subscribe({
      next: (shops) => {
        this.shopsPage = shops;
        console.log(this.shopsPage);
        this.loading = false;

        // SOLUCIÓN: Usar setTimeout para esperar un ciclo de renderizado
        setTimeout(() => {
          this.initMap();
        }, 10);
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  //Fly to selected shop in map
  locateShopOnMap(shop: Shop) {
    if (this.map) {
      this.map.flyTo([shop.latitude, shop.longitude], 14, {
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
        if (foundShop){
          this.selectedManager = foundShop.assignedManager;
        }
        if (this.selectedManager){
          this.visibleUnassignButton = true;
        }
        this.visibleAssignmentDialog = true;
        console.log(this.managers);
      },
      error: () => {
        this.visibleAssignmentDialog = true;
      }
    })
  }

  cancelAssignment(){
    this.selectedManager = undefined;
    this.visibleAssignmentDialog = false;
    this.visibleUnassignButton = false;
  }

  setManagerAssignment(id: string, userId: string | undefined, state: boolean){
    let managerId = userId;
    if (!managerId){
      managerId = '0';
    }
    this.shopService.assignManager(id, managerId, state).subscribe({
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

  protected readonly formatAddress = formatAddress;
}
