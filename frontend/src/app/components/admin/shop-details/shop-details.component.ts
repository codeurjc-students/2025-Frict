import { Component, OnInit, signal, effect, inject, AfterViewInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import * as L from 'leaflet';
import {MessageService} from 'primeng/api';
import {InputNumber} from 'primeng/inputnumber';
import {Divider} from 'primeng/divider';
import {TableModule} from 'primeng/table';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';

interface ShopDetailsDTO {
  id: number;
  referenceCode: string;
  name: string;
  address: string;
  city: string;
  imageUrl: string;
  managerName: string;
  contactPhone: string;
  totalValue: number;
  coordinates: { lat: number, lng: number };
}

interface TruckDTO {
  id: number;
  plate: string;
  status: 'AVAILABLE' | 'ON_ROUTE' | 'MAINTENANCE';
  currentLoad: number;
  maxCapacity: number;
  driverName: string;
  lat: number;
  lng: number;
  lastUpdate: string;
}

interface ShopStockDTO {
  id: number;
  productName: string;
  productRef: string;
  category: string;
  image: string;
  price: number;
  quantity: number;
  minStock: number;
  lastReplenishment: Date;
}

@Component({
  selector: 'app-shop-details',
  standalone: true,
  imports: [
    CommonModule, FormsModule, InputNumber, Divider, TableModule, IconField, InputIcon, InputText
  ],
  providers: [MessageService],
  templateUrl: './shop-details.component.html',
  styleUrl: './shop-details.component.css'
})
export class ShopDetailsComponent implements OnInit, AfterViewInit, OnDestroy {
  private router = inject(Router);
  private messageService = inject(MessageService);

  shop = signal<ShopDetailsDTO | null>(null);
  trucks = signal<TruckDTO[]>([]);
  stocks = signal<ShopStockDTO[]>([]);

  selectedStock = signal<ShopStockDTO | null>(null);
  replenishQuantity = signal<number>(50);

  private map: L.Map | undefined;
  private markers: L.Marker[] = [];

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    this.loadMockData();
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        this.initMap();
      }, 100);
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    if (!this.shop()) return;

    const shopCoords = this.shop()!.coordinates;
    this.map = L.map('fleet-map', { zoomControl: false }).setView([shopCoords.lat, shopCoords.lng], 12);

    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap'
    }).addTo(this.map);

    const shopIcon = L.icon({
      iconUrl: 'https://cdn-icons-png.flaticon.com/512/7509/7509698.png',
      iconSize: [40, 40], iconAnchor: [20, 40], popupAnchor: [0, -38]
    });
    L.marker([shopCoords.lat, shopCoords.lng], { icon: shopIcon }).addTo(this.map)
      .bindPopup(`<b>${this.shop()?.name}</b><br>Base de Operaciones`);

    this.updateTruckMarkers();
  }

  private updateTruckMarkers() {
    if (!this.map) return;

    const truckIcon = L.icon({
      iconUrl: 'https://cdn-icons-png.flaticon.com/512/2769/2769339.png',
      iconSize: [32, 32], iconAnchor: [16, 16]
    });

    this.trucks().forEach(truck => {
      L.marker([truck.lat, truck.lng], { icon: truckIcon })
        .addTo(this.map!)
        .bindPopup(`
            <div class="text-center">
                <b class="text-slate-800">${truck.plate}</b><br>
                <span class="text-xs text-slate-500">${truck.status}</span>
            </div>
         `);
    });
  }

  selectProductForReplenish(stock: ShopStockDTO) {
    this.selectedStock.set(stock);
    this.replenishQuantity.set(stock.minStock * 2);
  }

  confirmReplenish() {
    const stock = this.selectedStock();
    const qty = this.replenishQuantity();

    if (stock && qty > 0) {
      this.stocks.update(items => items.map(i => {
        if (i.id === stock.id) return { ...i, quantity: i.quantity + qty };
        return i;
      }));

      this.messageService.add({ severity: 'success', summary: 'Orden Procesada', detail: `+${qty} uds añadidas al inventario.` });
      this.replenishQuantity.set(0);
    }
  }

  goBack() { this.router.navigate(['/admin/shops']); }

  focusTruckOnMap(truck: TruckDTO) {
    if(this.map) {
      this.map.flyTo([truck.lat, truck.lng], 14);
    }
  }

  private loadMockData() {
    this.shop.set({
      id: 1,
      referenceCode: 'MAD-CNTR-01',
      name: 'Flagship Madrid Gran Vía',
      address: 'Calle Gran Vía 28',
      city: 'Madrid, España',
      imageUrl: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?q=80&w=1000&auto=format&fit=crop',
      managerName: 'Carlos Ruiz',
      contactPhone: '+34 912 345 678',
      totalValue: 245000.00,
      coordinates: { lat: 40.4200, lng: -3.7020 } // Madrid
    });

    this.trucks.set([
      { id: 1, plate: '8842-KLP', status: 'ON_ROUTE', currentLoad: 800, maxCapacity: 1000, driverName: 'J. Pérez', lat: 40.4250, lng: -3.6900, lastUpdate: 'Hace 2 min' },
      { id: 2, plate: '1122-BBC', status: 'AVAILABLE', currentLoad: 0, maxCapacity: 1200, driverName: 'A. García', lat: 40.4210, lng: -3.7010, lastUpdate: 'En Base' },
      { id: 3, plate: '9090-XYZ', status: 'ON_ROUTE', currentLoad: 450, maxCapacity: 1000, driverName: 'M. Lou', lat: 40.4100, lng: -3.7100, lastUpdate: 'Hace 5 min' },
    ]);

    this.stocks.set([
      { id: 1, productName: 'MacBook Pro M3', productRef: 'APL-M3-14', category: 'Portátiles', image: '', price: 2400, quantity: 3, minStock: 5, lastReplenishment: new Date() },
      { id: 2, productName: 'Sony WH-1000XM5', productRef: 'SNY-HD-05', category: 'Audio', image: '', price: 350, quantity: 45, minStock: 10, lastReplenishment: new Date() },
      { id: 3, productName: 'Logitech MX Master', productRef: 'LOG-MS-3S', category: 'Accesorios', image: '', price: 99, quantity: 0, minStock: 15, lastReplenishment: new Date() },
      { id: 4, productName: 'Samsung Odyssey G9', productRef: 'SAM-MON-49', category: 'Monitores', image: '', price: 1100, quantity: 8, minStock: 4, lastReplenishment: new Date() },
      { id: 5, productName: 'Keychron Q1 Pro', productRef: 'KEY-Q1-Red', category: 'Periféricos', image: '', price: 180, quantity: 22, minStock: 10, lastReplenishment: new Date() }
    ]);
  }
}
