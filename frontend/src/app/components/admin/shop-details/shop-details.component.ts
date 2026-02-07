import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core'; // IMPORTANTE: ChangeDetectorRef
import {isPlatformBrowser, NgClass, NgIf} from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import * as L from 'leaflet';
import { MessageService } from 'primeng/api';
import { ShopService } from '../../../services/shop.service';
import { TruckService } from '../../../services/truck.service';
import {Shop} from '../../../models/shop.model';
import {Truck} from '../../../models/truck.model';
import {ShopStock} from '../../../models/shopStock.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {InputGroup} from 'primeng/inputgroup';
import {InputText} from 'primeng/inputtext';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Avatar} from 'primeng/avatar';
import {Tag} from 'primeng/tag';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {FormsModule} from '@angular/forms';
import {InputNumber} from 'primeng/inputnumber';
import {Tooltip} from 'primeng/tooltip';
import {formatAddress} from '../../../utils/textFormat.util';
import {getTruckStatusTagInfo} from '../../../utils/tagManager.util';

@Component({
  selector: 'app-shop-details',
  standalone: true,
  imports: [
    LoadingScreenComponent,
    InputGroup,
    InputText,
    InputGroupAddon,
    Button,
    TableModule,
    Avatar,
    Tag,
    Paginator,
    NgClass,
    ToggleSwitch,
    FormsModule,
    InputNumber,
    NgIf,
    Tooltip
  ],
  templateUrl: './shop-details.component.html',
  styleUrl: 'shop-details.component.css'
})
export class ShopDetailsComponent implements OnInit, OnDestroy {

  shop!: Shop;

  trucksPage: PageResponse<Truck> = { items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 5 };
  firstTruck: number = 0;
  trucksRows: number = 5;

  stocksPage: PageResponse<ShopStock> = { items: [], totalItems: 0, currentPage: 0, lastPage: 0, pageSize: 5 };
  firstStock: number = 0;
  stocksRows: number = 5;

  selectedStock: ShopStock | undefined = undefined;
  restockQuantity: number = 0;

  loading: boolean = true;
  error: boolean = false;

  private map: L.Map | undefined;
  private markersLayer: L.LayerGroup | undefined;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private router: Router,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private shopService: ShopService,
    private truckService: TruckService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadData();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
  }

  // --- DATA LOAD MANAGEMENT ---
  loadData() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    this.loading = true;

    this.shopService.getShopById(id).subscribe({
      next: (shop) => {
        this.shop = shop;
        this.loadStocksPage();
        this.loadTrucksPage();
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  loadStocksPage() {
    if (!this.shop) return;

    this.shopService.getStocksPageByShopId(this.shop.id, this.firstStock / this.stocksRows, this.stocksRows).subscribe({
      next: (page) => {
        this.stocksPage = page;
        console.log(this.stocksPage);
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error cargando stock' })
    });
  }

  loadTrucksPage() {
    if (!this.shop) return;

    this.truckService.getTrucksPageByShopId(this.shop.id, this.firstTruck / this.trucksRows, this.trucksRows).subscribe({
      next: (page) => {
        this.trucksPage = page;
        console.log(this.trucksPage);

        //MAP LOADING
        this.loading = false;
        this.cdr.detectChanges();
        if (isPlatformBrowser(this.platformId)) {
          this.initMap();
        }
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error cargando camiones' });
      }
    });
  }

  // --- PAGINATION ---
  onStocksPageChange(event: PaginatorState) {
    this.firstStock = event.first ?? 0;
    this.stocksRows = event.rows ?? 5;
    this.loadStocksPage();
  }

  onTrucksPageChange(event: PaginatorState) {
    this.firstTruck = event.first ?? 0;
    this.trucksRows = event.rows ?? 5;

    // Al paginar camiones, recargamos datos y actualizamos marcadores
    this.truckService.getTrucksPageByShopId(this.shop.id, this.firstTruck / this.trucksRows, this.trucksRows).subscribe({
      next: (page) => {
        this.trucksPage = page;
        this.updateTruckMarkers(); // Solo actualizamos pines, no reiniciamos el mapa
      }
    });
  }

  // --- MAPA ---
  private initMap(): void {
    // Si no hay tienda o el mapa ya existe, salimos
    if (!this.shop || this.map) {
      if (this.map) this.updateTruckMarkers(); // Si ya existe, solo actualizamos pines
      return;
    }

    // Verificar que el elemento existe en el DOM
    const mapContainer = document.getElementById('fleet-map');
    if (!mapContainer) {
      console.error("Map container not found");
      return;
    }

    this.map = L.map('fleet-map', { zoomControl: false }).setView([this.shop.latitude, this.shop.longitude], 12);
    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: 'OpenStreetMap' }).addTo(this.map);

    // Crear LayerGroup para poder borrar marcadores luego
    this.markersLayer = L.layerGroup().addTo(this.map);

    const shopIcon = L.icon({
      iconUrl: './shopIcon.png', // Asegúrate que esta ruta es correcta en /public o /assets
      iconSize: [34, 34], iconAnchor: [18, 18]
    });

    L.marker([this.shop.latitude, this.shop.longitude], { icon: shopIcon }).addTo(this.map);

    this.updateTruckMarkers();
  }

  private updateTruckMarkers() {
    if (!this.map || !this.markersLayer) return;

    // 1. Limpiamos marcadores antiguos
    this.markersLayer.clearLayers();

    const truckIcon = L.icon({
      iconUrl: './truckIcon.png', // Asegúrate que esta ruta es correcta
      iconSize: [32, 32], iconAnchor: [16, 16]
    });

    // 2. Añadimos los nuevos
    this.trucksPage.items.forEach(t => {
      L.marker([t.latitude, t.longitude], { icon: truckIcon })
        .addTo(this.markersLayer!)
        .bindPopup(`<b>${t.referenceCode}</b><br>Estado: OK`); // Asumiendo que t.status existe
    });
  }

  // --- ACTIONS (Sin cambios) ---
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
      this.stocksPage.items = this.stocksPage.items.map(i => i.id === stock.id ? { ...i, quantity: i.units + qty } : i);
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
  protected readonly getTruckStatusTagInfo = getTruckStatusTagInfo;
}
