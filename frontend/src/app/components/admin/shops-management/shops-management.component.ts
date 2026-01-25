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

export interface ShopDTO {
  id: number;
  referenceCode: string;
  name: string;
  fullAddress: string;
  city: string;
  productsCount: number; // availableProducts.size()
  trucksCount: number;   // assignedTrucks.size()
  lat: number;
  lng: number;
}

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
    RouterLink, InputGroup, InputGroupAddon, Button, TableModule, Paginator, InputText, Tooltip
  ],
  templateUrl: './shops-management.component.html',
  styleUrl: 'shops-management.component.css'
})
export class ShopsManagementComponent implements OnInit, AfterViewInit, OnDestroy {

  shops = signal<ShopDTO[]>([]);
  selectedShop = signal<ShopDTO | null>(null);
  shopAlerts = signal<ShopAlert[]>([
    {
      shopName: 'Nombre de Tienda 1',
      message: 'Stock crítico (-15%)',
      severity: 'high',
      icon: 'pi pi-exclamation-triangle'
    },
    {
      shopName: 'Madrid Central',
      message: 'Nuevo camión asignado',
      severity: 'info',
      icon: 'pi pi-truck'
    },
    {
      shopName: 'Barcelona Port',
      message: 'Retraso en entrega #992',
      severity: 'medium',
      icon: 'pi pi-clock'
    },
    {
      shopName: 'Valencia Hub',
      message: 'Inventario completado',
      severity: 'info',
      icon: 'pi pi-check-circle'
    }
  ]);

  // Pagination
  first = 0;
  rows = 5;
  totalRecords = 0;

  // Leaflet map
  private map: L.Map | undefined;
  private markers: L.Marker[] = [];

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.generateMockData();
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.initMap();
    }
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
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(this.map);

    this.renderShopMarkers();
  }

  private renderShopMarkers() {
    if (!this.map) return;

    // Custom shops icon
    const shopIcon = L.icon({
      iconUrl: 'https://cdn-icons-png.flaticon.com/512/7509/7509698.png',
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -28]
    });

    this.shops().forEach(shop => {
      const marker = L.marker([shop.lat, shop.lng], { icon: shopIcon })
        .addTo(this.map!)
        .bindPopup(`
            <div class="p-2 min-w-[140px] text-center">
                <h4 class="font-bold text-slate-800 text-sm">${shop.name}</h4>
                <p class="text-xs text-slate-500 mb-2">${shop.city}</p>
                <span class="bg-cyan-100 text-cyan-800 text-[10px] px-2 py-0.5 rounded-full font-bold">
                    Stock: ${shop.productsCount}
                </span>
            </div>
        `);

      this.markers.push(marker);
    });
  }

  //Fly to selected shop in map
  locateShopOnMap(shop: ShopDTO) {
    if (this.map) {
      this.map.flyTo([shop.lat, shop.lng], 14, {
        duration: 1.5 //Seconds
      });
    }
  }

  onManageShop(shop: ShopDTO) {
    console.log("Navegando a gestión de tienda:", shop.id);
  }

  onPageChange(event: any) {
    this.first = event.first;
    this.rows = event.rows;
  }

  // --- Helpers & Mock Data ---
  private generateMockData() {
    const cities = [
      { name: 'Madrid', lat: 40.4168, lng: -3.7038 },
      { name: 'Barcelona', lat: 41.3851, lng: 2.1734 },
      { name: 'Valencia', lat: 39.4699, lng: -0.3763 },
      { name: 'Sevilla', lat: 37.3891, lng: -5.9845 },
      { name: 'Bilbao', lat: 43.2630, lng: -2.9350 }
    ];
    this.shops.set(cities.map((city, i) => ({
      id: i + 1,
      referenceCode: `SHOP-${100+i}`,
      name: `Tienda ${city.name}`,
      city: city.name,
      fullAddress: `Polígono Ind. ${city.name}, Nave ${i+4}`,
      productsCount: Math.floor(Math.random() * 5000) + 1000,
      trucksCount: Math.floor(Math.random() * 8) + 1,
      lat: city.lat,
      lng: city.lng
    })));
    this.totalRecords = 5;
  }
}
