import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import * as L from 'leaflet';

import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { User } from '../../../models/user.model';
import { Address } from '../../../models/address.model';
import { PageResponse } from '../../../models/pageResponse.model';
import {Button} from 'primeng/button';
import {SelectButton} from 'primeng/selectbutton';
import {UIChart} from 'primeng/chart';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {InputText} from 'primeng/inputtext';
import {TableModule} from 'primeng/table';
import {Avatar} from 'primeng/avatar';
import {Tag} from 'primeng/tag';
import {ProgressBar} from 'primeng/progressbar';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {Dialog} from 'primeng/dialog';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {Select} from 'primeng/select';
import {MessageService} from 'primeng/api';

export interface TruckDTO {
  id: string;
  referenceCode: string;
  plateNumber: string;
  status: 'AVAILABLE' | 'ON_ROUTE' | 'MAINTENANCE' | 'OUT_OF_SERVICE';
  maxOrderCapacity: number;
  shopId: string;
  assignedDriver: User | null;
  address: Address;
  activeOrdersToDeliver: number;
}

interface TruckAlert {
  truckRef: string;
  message: string;
  severity: 'high' | 'medium' | 'info';
  icon: string;
}

@Component({
  selector: 'app-trucks-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, LoadingScreenComponent, Button, SelectButton, UIChart, InputGroup, InputGroupAddon, InputText, TableModule, Avatar, Tag, ProgressBar, Paginator, Dialog, Tabs, TabList, Tab, TabPanels, TabPanel, Select
  ],
  templateUrl: './trucks-management.component.html'
})
export class TrucksManagementComponent implements OnInit, OnDestroy {

  trucksPage: PageResponse<TruckDTO> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };
  first = 0;
  rows = 5;
  globalFilter: string = '';

  isInitialLoad: boolean = true;
  loading: boolean = true;
  tableLoading: boolean = false;
  error: boolean = false;

  // KPIs
  totalTrucks: number = 0;
  onRouteTrucks: number = 0;
  noDriverTrucks: number = 0;
  maintenanceTrucks: number = 0;

  truckAlerts = signal<TruckAlert[]>([
    { truckRef: 'TRK-003', message: 'Avería mecánica reportada', severity: 'high', icon: 'pi pi-wrench' },
    { truckRef: 'TRK-001', message: 'Retraso estimado 20 min', severity: 'medium', icon: 'pi pi-clock' },
    { truckRef: 'TRK-006', message: 'Sistema de frío inestable', severity: 'high', icon: 'pi pi-bolt' },
    { truckRef: 'TRK-002', message: 'Entrega completada con éxito', severity: 'info', icon: 'pi pi-check-circle' }
  ]);

  viewModeOptions = [
    { label: 'Mapa', value: 'map', icon: 'pi pi-map' },
    { label: 'Distribución', value: 'chart', icon: 'pi pi-chart-pie' },
    { label: 'Notificaciones', value: 'alerts', icon: 'pi pi-bell' }
  ];
  selectedViewMode: string = 'map';

  chartData: any;
  chartOptions: any;

  displayTruckDialog: boolean = false;
  selectedTruck: TruckDTO | null = null;
  isEditing: boolean = false;

  private map: L.Map | undefined;
  private markers: L.Marker[] = [];

  statusOptions = [
    { label: 'Disponible', value: 'AVAILABLE' },
    { label: 'En Ruta', value: 'ON_ROUTE' },
    { label: 'En Mantenimiento', value: 'MAINTENANCE' },
    { label: 'Fuera de Servicio', value: 'OUT_OF_SERVICE' }
  ];

  private allMockTrucks: TruckDTO[] = [];

  constructor(private messageService: MessageService) {}

  ngOnInit() {
    this.generateMockData();
    this.initChartOptions();
    this.loadTrucks();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  loadTrucks() {
    if (this.isInitialLoad) {
      this.loading = true;
    } else {
      this.tableLoading = true;
    }

    setTimeout(() => {
      let filtered = this.allMockTrucks;
      if (this.globalFilter) {
        const q = this.globalFilter.toLowerCase();
        filtered = filtered.filter(t => t.plateNumber.toLowerCase().includes(q) || t.referenceCode.toLowerCase().includes(q));
      }

      this.totalTrucks = filtered.length;
      this.onRouteTrucks = filtered.filter(t => t.status === 'ON_ROUTE').length;
      this.noDriverTrucks = filtered.filter(t => !t.assignedDriver).length;
      this.maintenanceTrucks = filtered.filter(t => t.status === 'MAINTENANCE').length;

      this.trucksPage = {
        items: filtered.slice(this.first, this.first + this.rows),
        totalItems: filtered.length,
        currentPage: this.first / this.rows,
        lastPage: Math.ceil(filtered.length / this.rows) - 1,
        pageSize: this.rows
      };

      this.updateChartData(filtered);
      this.loading = false;
      this.tableLoading = false;
      this.isInitialLoad = false;

      setTimeout(() => {
        if (!this.map && this.selectedViewMode === 'map') {
          this.initMap();
        }
        this.renderTruckMarkers();
      }, 50);

    }, 400);
  }

  onPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 5;
    this.loadTrucks();
  }

  onSearch() {
    this.first = 0;
    this.loadTrucks();
  }

  onViewModeChange() {
    if (this.selectedViewMode === 'map') {
      setTimeout(() => {
        if (!this.map) this.initMap();
        this.map?.invalidateSize();
        this.renderTruckMarkers();
      }, 50);
    }
  }

  private initChartOptions() {
    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          align: 'center',
          labels: {
            usePointStyle: true,
            padding: 25,
            color: '#475569',
            font: { weight: 'bold', size: 12 }
          }
        }
      },
      layout: {
        padding: { top: 10, bottom: 10, left: 10, right: 10 }
      }
    };
  }

  private updateChartData(filteredData: TruckDTO[]) {
    const available = filteredData.filter(t => t.status === 'AVAILABLE').length;
    const onRoute = filteredData.filter(t => t.status === 'ON_ROUTE').length;
    const maintenance = filteredData.filter(t => t.status === 'MAINTENANCE').length;
    const outOfService = filteredData.filter(t => t.status === 'OUT_OF_SERVICE').length;

    this.chartData = {
      labels: ['Disponibles', 'En Ruta', 'Mantenimiento', 'Inactivos'],
      datasets: [
        {
          data: [available, onRoute, maintenance, outOfService],
          backgroundColor: ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444'],
          hoverOffset: 15,
          borderWidth: 0
        }
      ]
    };
  }

  private initMap(): void {
    const mapEl = document.getElementById('trucks-map');
    if (!mapEl) return;
    this.map = L.map('trucks-map', { zoomControl: false }).setView([40.4168, -3.7038], 6);
    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OpenStreetMap', maxZoom: 19 }).addTo(this.map);
  }

  private renderTruckMarkers() {
    if (!this.map) return;
    this.markers.forEach(m => m.remove());
    this.markers = [];
    const bounds = L.latLngBounds([]);
    let hasCoords = false;

    this.trucksPage.items.forEach(truck => {
      if (truck.address.latitude && truck.address.longitude) {
        hasCoords = true;
        const pinColor = truck.status === 'ON_ROUTE' ? '#3b82f6' : truck.status === 'AVAILABLE' ? '#22c55e' : '#ef4444';
        const customIcon = L.divIcon({
          className: 'custom-pin',
          html: `<div style="background-color: ${pinColor}; width: 22px; height: 22px; border-radius: 50%; border: 3px solid white; box-shadow: 0 4px 6px rgba(0,0,0,0.3);"></div>`,
          iconSize: [22, 22], iconAnchor: [11, 11]
        });
        const marker = L.marker([truck.address.latitude, truck.address.longitude], { icon: customIcon })
          .addTo(this.map!)
          .bindPopup(`<div class="text-center font-sans"><strong>${truck.plateNumber}</strong><br><span class="text-xs text-slate-500">${truck.assignedDriver?.name || 'Sin asignar'}</span></div>`);
        this.markers.push(marker);
        bounds.extend([truck.address.latitude, truck.address.longitude]);
      }
    });
    if (hasCoords) this.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 12 });
  }

  locateTruckOnMap(truck: TruckDTO) {
    this.selectedViewMode = 'map';
    this.onViewModeChange();
    setTimeout(() => {
      if (this.map && truck.address.latitude && truck.address.longitude) {
        this.map.flyTo([truck.address.latitude, truck.address.longitude], 14, { duration: 1.5 });
      }
    }, 100);
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = { 'AVAILABLE': 'Disponible', 'ON_ROUTE': 'En Ruta', 'MAINTENANCE': 'Mantenimiento', 'OUT_OF_SERVICE': 'Fuera de Servicio' };
    return labels[status] || status;
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    const severities: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast'> = { 'AVAILABLE': 'success', 'ON_ROUTE': 'info', 'MAINTENANCE': 'warn', 'OUT_OF_SERVICE': 'danger' };
    return severities[status] || 'secondary';
  }

  getLoadPercentage(active: number, max: number): number {
    return max === 0 ? 0 : Math.round((active / max) * 100);
  }

  openNew() {
    this.selectedTruck = {
      id: '', referenceCode: '', plateNumber: '', status: 'AVAILABLE', maxOrderCapacity: 0, shopId: '', assignedDriver: null, activeOrdersToDeliver: 0,
      address: { id: '', alias: '', street: '', number: '', floor: '', postalCode: '', city: '', country: '' }
    };
    this.isEditing = false;
    this.displayTruckDialog = true;
  }

  editTruck(truck: TruckDTO) {
    this.selectedTruck = { ...truck };
    this.isEditing = true;
    this.displayTruckDialog = true;
  }

  deleteTruck(truckId: string) {
    this.allMockTrucks = this.allMockTrucks.filter(t => t.id !== truckId);
    this.onSearch();
    this.messageService.add({ severity: 'success', summary: 'Eliminado', detail: `Camión eliminado.` });
  }

  saveTruck() {
    this.displayTruckDialog = false;
    this.loadTrucks();
    this.messageService.add({ severity: 'success', summary: 'Guardado', detail: 'Datos actualizados.' });
  }

  private generateMockData() {
    const mockDriver = (id: string, name: string, username: string) => ({
      id, name, username, roles: ['DRIVER'], email: `${username}@fleet.com`,
      phone: '', addresses: [], cards: [], banned: false, deleted: false,
      logged: false, ordersCount: 0, favouriteProductsCount: 0,
      lastConnection: '', selectedShopId: null,
      imageInfo: {
        id: `img-${id}`,
        imageUrl: `https://i.pravatar.cc/150?u=${id}`,
        s3Key: '', fileName: ''
      }
    });

    this.allMockTrucks = [
      // --- Tus datos originales (mantenidos) ---
      { id: '1', referenceCode: 'TRK-001', plateNumber: '1234-ABC', status: 'ON_ROUTE', maxOrderCapacity: 20, shopId: '1', activeOrdersToDeliver: 15, assignedDriver: mockDriver('d1', 'Carlos Ruiz', 'carlos.r'), address: { id: 'a1', alias: 'Ruta 1', street: 'M-40', number: '12', floor: '', postalCode: '28000', city: 'Madrid', country: 'España', latitude: 40.4168, longitude: -3.7038 } },
      { id: '2', referenceCode: 'TRK-002', plateNumber: '5678-DEF', status: 'AVAILABLE', maxOrderCapacity: 15, shopId: '1', activeOrdersToDeliver: 0, assignedDriver: mockDriver('d2', 'Ana Gómez', 'ana.g'), address: { id: 'a2', alias: 'Base 2', street: 'Calle Sur', number: '1', floor: '', postalCode: '28900', city: 'Getafe', country: 'España', latitude: 40.3083, longitude: -3.7327 } },
      { id: '3', referenceCode: 'TRK-003', plateNumber: '9012-GHI', status: 'MAINTENANCE', maxOrderCapacity: 25, shopId: '2', activeOrdersToDeliver: 0, assignedDriver: null, address: { id: 'a3', alias: 'Taller', street: 'Calle Taller', number: '4', floor: '', postalCode: '28021', city: 'Madrid', country: 'España', latitude: 40.35, longitude: -3.71 } },
      { id: '4', referenceCode: 'TRK-004', plateNumber: '3344-JKL', status: 'ON_ROUTE', maxOrderCapacity: 10, shopId: '1', activeOrdersToDeliver: 8, assignedDriver: mockDriver('d3', 'Luis Pérez', 'luis.p'), address: { id: 'a4', alias: 'Entrega 4', street: 'Gran Vía', number: '22', floor: '', postalCode: '28013', city: 'Madrid', country: 'España', latitude: 40.42, longitude: -3.70 } },
      { id: '5', referenceCode: 'TRK-005', plateNumber: '5566-MNO', status: 'AVAILABLE', maxOrderCapacity: 30, shopId: '3', activeOrdersToDeliver: 0, assignedDriver: null, address: { id: 'a5', alias: 'Norte', street: 'Av Norte', number: '10', floor: '', postalCode: '28100', city: 'Alcobendas', country: 'España', latitude: 40.53, longitude: -3.63 } },

      // --- Nuevos datos añadidos ---
      {
        id: '6', referenceCode: 'TRK-006', plateNumber: '7788-PQR', status: 'ON_ROUTE',
        maxOrderCapacity: 22, shopId: '2', activeOrdersToDeliver: 20,
        assignedDriver: mockDriver('d4', 'Elena Beltrán', 'elena.b'),
        address: { id: 'a6', alias: 'Ruta Sur', street: 'Av. Andalucía', number: '5', floor: '', postalCode: '28041', city: 'Madrid', country: 'España', latitude: 40.37, longitude: -3.69 }
      },
      {
        id: '7', referenceCode: 'TRK-007', plateNumber: '9900-STU', status: 'AVAILABLE',
        maxOrderCapacity: 18, shopId: '1', activeOrdersToDeliver: 0,
        assignedDriver: mockDriver('d5', 'Roberto Sanz', 'roberto.s'),
        address: { id: 'a7', alias: 'Parking Central', street: 'Paseo Castellana', number: '100', floor: '', postalCode: '28046', city: 'Madrid', country: 'España', latitude: 40.44, longitude: -3.68 }
      },
      {
        id: '8', referenceCode: 'TRK-008', plateNumber: '1122-VWX', status: 'MAINTENANCE',
        maxOrderCapacity: 12, shopId: '4', activeOrdersToDeliver: 0,
        assignedDriver: null,
        address: { id: 'a8', alias: 'Taller Leganés', street: 'Calle Hierro', number: '14', floor: '', postalCode: '28914', city: 'Leganés', country: 'España', latitude: 40.32, longitude: -3.76 }
      },
      {
        id: '9', referenceCode: 'TRK-009', plateNumber: '3344-YZA', status: 'ON_ROUTE',
        maxOrderCapacity: 25, shopId: '1', activeOrdersToDeliver: 5,
        assignedDriver: mockDriver('d6', 'Marta López', 'marta.l'),
        address: { id: 'a9', alias: 'Ruta Este', street: 'Av. América', number: '40', floor: '', postalCode: '28002', city: 'Madrid', country: 'España', latitude: 40.43, longitude: -3.67 }
      },
      {
        id: '10', referenceCode: 'TRK-010', plateNumber: '5566-BCD', status: 'AVAILABLE',
        maxOrderCapacity: 40, shopId: '2', activeOrdersToDeliver: 0,
        assignedDriver: mockDriver('d7', 'Javier Ruiz', 'javier.r'),
        address: { id: 'a10', alias: 'Base Logística', street: 'C. Ind. Vallecas', number: '2', floor: '', postalCode: '28031', city: 'Madrid', country: 'España', latitude: 40.36, longitude: -3.62 }
      }
    ];
  }
}
