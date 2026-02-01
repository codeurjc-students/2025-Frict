import { Component, OnInit, signal, inject, AfterViewInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import * as L from 'leaflet';
import {MessageService} from 'primeng/api';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Avatar} from 'primeng/avatar';
import {Tag} from 'primeng/tag';
import {ProgressBar} from 'primeng/progressbar';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {InputNumber} from 'primeng/inputnumber';

// --- Interfaces Actualizadas ---
interface ShopDetailsDTO {
  id: number;
  referenceCode: string;
  name: string;
  address: string;
  city: string;
  imageUrl: string;
  managerName: string;
  totalValue: number;
  coordinates: { lat: number, lng: number };
  // Capacidad de almacenamiento
  currentStorage: number;
  maxStorage: number;
}

interface TruckDTO {
  id: number;
  referenceCode: string; // Ref camión
  plate: string;
  status: 'AVAILABLE' | 'ON_ROUTE' | 'MAINTENANCE';
  currentOrderId: string | null; // Pedido actual
  driver: { name: string, imageUrl: string } | null; // Usuario asignado
  lat: number;
  lng: number;
}

interface ShopStockDTO {
  id: number;
  active: boolean; // Toggle activación local
  productName: string;
  productRef: string;
  category: string;
  image: string;
  price: number;
  quantity: number; // Stock total
  maxLimit: number; // Límite de unidades
  minStock: number;
}

@Component({
  selector: 'app-shop-details',
  standalone: true,
  imports: [
    CommonModule, FormsModule, InputGroup, InputGroupAddon, Button, TableModule, Avatar, Tag, ProgressBar, ToggleSwitch, InputNumber,
  ],
  templateUrl: './shop-details.component.html',
  styles: [`
    :host { display: block; }
    #fleet-map { z-index: 0; }
  `]
})
export class ShopDetailsComponent implements OnInit, AfterViewInit, OnDestroy {
  private router = inject(Router);
  private messageService = inject(MessageService);

  // Signals
  shop = signal<ShopDetailsDTO | null>(null);
  trucks = signal<TruckDTO[]>([]);
  stocks = signal<ShopStockDTO[]>([]);

  // Reposición (Master-Detail)
  selectedStock = signal<ShopStockDTO | null>(null);
  replenishQuantity = signal<number>(50);

  // Mapa
  private map: L.Map | undefined;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    this.loadMockData();
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => this.initMap(), 100);
    }
  }

  ngOnDestroy(): void {
    if (this.map) this.map.remove();
  }

  // --- MAPA ---
  private initMap(): void {
    if (!this.shop()) return;
    const coords = this.shop()!.coordinates;

    this.map = L.map('fleet-map', { zoomControl: false }).setView([coords.lat, coords.lng], 12);
    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OSM' }).addTo(this.map);

    // Marker Tienda
    const shopIcon = L.icon({
      iconUrl: 'https://cdn-icons-png.flaticon.com/512/7509/7509698.png',
      iconSize: [40, 40], iconAnchor: [20, 40], popupAnchor: [0, -38]
    });
    L.marker([coords.lat, coords.lng], { icon: shopIcon }).addTo(this.map);

    this.updateTruckMarkers();
  }

  private updateTruckMarkers() {
    if (!this.map) return;
    const truckIcon = L.icon({
      iconUrl: 'https://cdn-icons-png.flaticon.com/512/2769/2769339.png',
      iconSize: [32, 32], iconAnchor: [16, 16]
    });

    this.trucks().forEach(t => {
      L.marker([t.lat, t.lng], { icon: truckIcon }).addTo(this.map!)
        .bindPopup(`<b>${t.referenceCode}</b><br>${t.status}`);
    });
  }

  // --- ACCIONES ---

  calculateStorageUsage(): number {
    const s = this.shop();
    if (!s || s.maxStorage === 0) return 0;
    return Math.round((s.currentStorage / s.maxStorage) * 100);
  }

  toggleStockActive(stock: ShopStockDTO, event: any) {
    // Lógica backend para desactivar stock en esta tienda
    this.messageService.add({ severity: 'info', summary: 'Estado actualizado', detail: `${stock.productName} ahora está ${stock.active ? 'Activo' : 'Inactivo'}` });
  }

  selectProductForReplenish(stock: ShopStockDTO) {
    this.selectedStock.set(stock);
    this.replenishQuantity.set(stock.minStock);
  }

  confirmReplenish() {
    const stock = this.selectedStock();
    const qty = this.replenishQuantity();
    if (stock && qty > 0) {
      // Update optimista
      this.stocks.update(items => items.map(i => i.id === stock.id ? { ...i, quantity: i.quantity + qty } : i));
      this.messageService.add({ severity: 'success', summary: 'Stock reponido', detail: `+${qty} unidades` });
      this.replenishQuantity.set(0);
    }
  }

  deleteStock(id: number) {
    this.messageService.add({severity: 'warn', summary: 'Eliminar', detail: 'Funcionalidad de borrado simulada'});
  }

  deleteTruck(id: number) {
    this.messageService.add({severity: 'error', summary: 'Eliminar Camión', detail: 'Camión desvinculado'});
  }

  focusTruckOnMap(truck: TruckDTO) {
    if(this.map) this.map.flyTo([truck.lat, truck.lng], 14);
  }

  goBack() { this.router.navigate(['/admin/shops']); }

  // --- MOCK DATA ---
  private loadMockData() {
    this.shop.set({
      id: 1, referenceCode: 'MAD-001', name: 'Madrid Gran Vía Flagship',
      address: 'C/ Gran Vía 28', city: 'Madrid', imageUrl: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?q=80&w=1000&auto=format&fit=crop',
      managerName: 'Carlos Ruiz', totalValue: 245000,
      coordinates: { lat: 40.4200, lng: -3.7020 },
      currentStorage: 3500, maxStorage: 5000 // Para el progress bar
    });

    this.trucks.set([
      { id: 1, referenceCode: 'TRK-8842', plate: '8842-KLP', status: 'ON_ROUTE', currentOrderId: '#ORD-9921', driver: {name: 'Juan P.', imageUrl: ''}, lat: 40.4250, lng: -3.6900 },
      { id: 2, referenceCode: 'TRK-1122', plate: '1122-BBC', status: 'AVAILABLE', currentOrderId: null, driver: null, lat: 40.4210, lng: -3.7010 },
      { id: 3, referenceCode: 'TRK-9090', plate: '9090-XYZ', status: 'MAINTENANCE', currentOrderId: null, driver: {name: 'M. Lou', imageUrl: ''}, lat: 40.4100, lng: -3.7100 },
    ]);

    this.stocks.set([
      { id: 1, active: true, productName: 'MacBook Pro M3', productRef: 'APL-001', category: 'Portátiles', image: '', price: 2400, quantity: 3, maxLimit: 50, minStock: 5 },
      { id: 2, active: true, productName: 'Sony WH-1000XM5', productRef: 'SNY-002', category: 'Audio', image: '', price: 350, quantity: 150, maxLimit: 200, minStock: 10 },
      { id: 3, active: false, productName: 'Logitech MX Master', productRef: 'LOG-003', category: 'Accesorios', image: '', price: 99, quantity: 0, maxLimit: 100, minStock: 15 },
    ]);
  }
}
