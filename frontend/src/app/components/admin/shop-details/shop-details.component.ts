import { Component, OnInit, signal, inject, AfterViewInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';

import * as L from 'leaflet';
import {MessageService} from 'primeng/api';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Avatar} from 'primeng/avatar';
import {Tag} from 'primeng/tag';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {InputNumber} from 'primeng/inputnumber';
import {Tooltip} from 'primeng/tooltip';
import {InputText} from 'primeng/inputtext';
import {Shop} from '../../../models/shop.model';
import {Truck} from '../../../models/truck.model';
import {ShopStock} from '../../../models/shopStock.model';
import {ShopService} from '../../../services/shop.service';
import {formatAddress} from '../../../utils/textFormat.util';


@Component({
  selector: 'app-shop-details',
  standalone: true,
  imports: [
    CommonModule, FormsModule, InputGroup, InputGroupAddon, Button, TableModule, Avatar, Tag, ToggleSwitch, InputNumber, Tooltip, InputText,
  ],
  templateUrl: './shop-details.component.html',
  styleUrl: 'shop-details.component.css'
})
export class ShopDetailsComponent implements OnInit, AfterViewInit, OnDestroy {

  shop!: Shop;
  trucks: Truck[] = [];
  stocks: ShopStock[] = [];

  selectedStock: ShopStock | undefined = undefined;
  restockQuantity: number = 0;

  private map: L.Map | undefined;

  constructor(@Inject(PLATFORM_ID) private platformId: Object,
              private router: Router,
              private route: ActivatedRoute,
              private messageService: MessageService,
              private shopService: ShopService) {}

  ngOnInit() {
    this.loadShop();
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => this.initMap(), 100);
    }
  }

  ngOnDestroy(): void {
    if (this.map) this.map.remove();
  }


  // --- LOAD DATA ---
  loadShop() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id){
      this.shopService.getShopById(id).subscribe({
        next: (shop) => {
          this.shop = shop;
          this.loadStocksPage();
          this.loadTrucks();
        }
      })
    }
  }

  loadStocksPage() {
    //Implement
  }

  loadTrucks() {
    //Implement
  }

  // --- MAP ---
  private initMap(): void {
    if (!this.shop) return;

    this.map = L.map('fleet-map', { zoomControl: false }).setView([this.shop.latitude, this.shop.longitude], 12);
    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OSM' }).addTo(this.map);

    const shopIcon = L.icon({
      iconUrl: './shopIcon.png',
      iconSize: [34, 34], iconAnchor: [18, 18]
    });
    L.marker([this.shop.latitude, this.shop.longitude], { icon: shopIcon }).addTo(this.map);

    this.updateTruckMarkers();
  }

  private updateTruckMarkers() {
    if (!this.map) return;
    const truckIcon = L.icon({
      iconUrl: './truckIcon.png',
      iconSize: [32, 32], iconAnchor: [16, 16]
    });

    this.trucks.forEach(t => {
      L.marker([t.latitude, t.longitude], { icon: truckIcon }).addTo(this.map!)
        .bindPopup(`<b>${t.referenceCode}</b><br>Estado`);
    });
  }


  // --- ACTIONS ---
/*
  calculateStorageUsage(): number {
    const s = this.shop;
    if (!s || s.maxStorage === 0) return 0;
    return Math.round((s.stock / s.maxStorage) * 100);
  }
*/
  toggleStockActive(stock: ShopStock) {
    this.messageService.add({ severity: 'info', summary: 'Estado actualizado', detail: `${stock.productId} ahora está ${stock.active ? 'Activo' : 'Inactivo'}` });
  }

  selectProductForReplenish(stock: ShopStock) {
    this.selectedStock = stock;
    this.restockQuantity = 0;
  }

  confirmReplenish() {
    const stock = this.selectedStock;
    const qty = this.restockQuantity;

    if (stock && qty > 0) {
      this.stocks = this.stocks.map(i => i.id === stock.id ? { ...i, quantity: i.units + qty } : i);
      this.messageService.add({severity: 'success', summary: 'Stock repuesto', detail: `+${qty} unidades`});
      this.restockQuantity = 0;
    }
  }

  deleteStock(id: number) {
    this.messageService.add({severity: 'warn', summary: 'Eliminar', detail: 'Funcionalidad de borrado simulada'});
  }

  deleteTruck(id: number) {
    this.messageService.add({severity: 'error', summary: 'Eliminar Camión', detail: 'Camión desvinculado'});
  }

  focusTruckOnMap(truck: Truck) {
    if(this.map) this.map.flyTo([truck.latitude, truck.longitude], 14);
  }

  goBack() { this.router.navigate(['/admin/shops']); }

  protected readonly formatAddress = formatAddress;
}
