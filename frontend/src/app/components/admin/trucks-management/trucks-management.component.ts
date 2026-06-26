import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import * as L from 'leaflet';

import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {User} from '../../../models/user.model';
import {PageResponse} from '../../../models/pageResponse.model';
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
import {ConfirmationService, MessageService} from 'primeng/api';
import {Alert} from '../../../utils/ui.service';

import {Tooltip} from 'primeng/tooltip';
import {Textarea} from 'primeng/textarea';
import {Truck} from '../../../models/truck.model';
import {TruckService} from '../../../services/truck.service';
import {formatAddress, formatDuration} from '../../../utils/textFormat.util';
import {getTruckHistoryStatusTagInfo} from '../../../utils/tagManager.util';
import {UserService} from '../../../services/user.service';
import {Select} from 'primeng/select';
import {RouterLink} from '@angular/router';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {LocationService} from '../../../services/location.service';
import {DriverLocationPingService} from '../../../services/driver-location-ping.service';
import {DriverLocation} from '../../../models/driver-location.model';

@Component({
  selector: 'app-trucks-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, LoadingScreenComponent, Button, SelectButton, UIChart, InputGroup, InputGroupAddon, InputText, TableModule, Avatar, Tag, ProgressBar, Paginator, Dialog, Select, Tooltip, Textarea, RouterLink, BreadcrumbReloadComponent
  ],
  templateUrl: './trucks-management.component.html'
})
export class TrucksManagementComponent implements OnInit, OnDestroy {

  private truckService = inject(TruckService);
  private messageService = inject(MessageService);
  private userService = inject(UserService);
  private confirmationService = inject(ConfirmationService);
  private locationService = inject(LocationService);
  private driverLocationPingService = inject(DriverLocationPingService);

  trucksPage: PageResponse<Truck> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };
  first = 0;
  rows = 5;
  globalFilter: string = '';

  isInitialLoad: boolean = true;
  loading: boolean = true;
  error: boolean = false;

  // KPIs
  totalTrucks: number = 0;
  onRouteTrucks: number = 0;
  noDriverTrucks: number = 0;
  maintenanceTrucks: number = 0;

  truckAlerts = signal<Alert[]>([
    { reference: 'TRK-003', message: 'Avería mecánica reportada', severity: 'high', icon: 'pi pi-wrench' },
    { reference: 'TRK-001', message: 'Retraso estimado 20 min', severity: 'medium', icon: 'pi pi-clock' },
    { reference: 'TRK-006', message: 'Sistema de frío inestable', severity: 'high', icon: 'pi pi-bolt' },
    { reference: 'TRK-002', message: 'Entrega completada con éxito', severity: 'info', icon: 'pi pi-check-circle' }
  ]);

  viewModeOptions = [
    { label: 'Mapa', value: 'map', icon: 'pi pi-map' },
    { label: 'Distribución', value: 'chart', icon: 'pi pi-chart-pie' }
  ];
  selectedViewMode: string = 'map';

  chartData: any = { labels: [], datasets: [] };
  assignedChartData: any = { labels: [], datasets: [] };
  chartOptions: any;

  selectedTruck: Truck | null = null;

  // History dialog state
  displayHistoryDialog: boolean = false;
  newHistoryStatus: string = '';
  newHistoryComment: string = '';

  // Driver assignment dialog state
  displayAssignmentDialog: boolean = false;
  drivers: User[] = [];
  selectedDriver: User | undefined = undefined;
  currentTruckForAssignment: Truck | undefined = undefined;

  private map: L.Map | undefined;
  private markers: L.Marker[] = [];
  private driverMarkers: L.Marker[] = [];
  private routePolylines: L.Polyline[] = [];
  private allDriverLocations: DriverLocation[] = [];

  statusOptions = [
    { label: 'Descanso',           value: 'Descanso' },
    { label: 'En ruta a la tienda', value: 'En ruta a la tienda' },
    { label: 'En Reparto',         value: 'En Reparto' },
    { label: 'Fuera de Servicio',  value: 'Fuera de servicio' }
  ];

  ngOnInit() {
    this.initChartOptions();
    this.loadTrucks();
    this.driverLocationPingService.getAllDriverLocations().subscribe({
      next: (locs) => { this.allDriverLocations = locs; this.renderDriverMarkers(); }
    });
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  public reloadAll() {
    this.loading = true;
    this.error = false;

    // 1. Leaflet map cleaning
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
    this.markers = [];
    this.driverMarkers = [];
    this.routePolylines = [];

    // 2. Modals closing and selections cleaning
    this.displayHistoryDialog = false;
    this.displayAssignmentDialog = false;
    this.selectedTruck = null;
    this.selectedDriver = undefined;

    // 3. Send requests
    this.loadTrucks();
    this.driverLocationPingService.getAllDriverLocations().subscribe({
      next: (locs) => { this.allDriverLocations = locs; this.renderDriverMarkers(); }
    });
  }

  loadTrucks() {
    if (this.isInitialLoad) {
      this.loading = true;
    }

    this.truckService.getAllTrucksPage(this.first / this.rows, this.rows).subscribe({
      next: (page) => {
        this.trucksPage = page;
        this.calculateKPIs(page.items);
        this.updateChartData(page.items);

        this.loading = false;
        this.isInitialLoad = false;

        setTimeout(() => {
          if (!this.map && this.selectedViewMode === 'map') {
            this.initMap();
          }
          this.renderTruckMarkers();
          this.renderDriverMarkers();
        }, 50);
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  calculateKPIs(items: Truck[]) {
    this.totalTrucks = this.trucksPage.totalItems;
    this.onRouteTrucks = items.filter(t => {
      const s = this.getCurrentStatus(t);
      return s === 'En ruta a la tienda' || s === 'En Reparto';
    }).length;
    this.noDriverTrucks = items.filter(t => !t.assignedDriver).length;
    this.maintenanceTrucks = items.filter(t => this.getCurrentStatus(t) === 'Fuera de servicio').length;
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
        this.renderDriverMarkers();
      }, 50);
    }
  }

  private initChartOptions() {
    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          align: 'center',
          labels: {
            usePointStyle: true,
            padding: 20,
            color: '#475569',
            font: { weight: 'bold', size: 12 }
          }
        }
      },
      layout: {
        padding: 20
      }
    };
  }

  private updateChartData(items: Truck[]) {
    const resting = items.filter(t => this.getCurrentStatus(t) === 'Descanso').length;
    const onRouteToShop = items.filter(t => this.getCurrentStatus(t) === 'En ruta a la tienda').length;
    const inDelivery = items.filter(t => this.getCurrentStatus(t) === 'En Reparto').length;
    const outOfService = items.filter(t => this.getCurrentStatus(t) === 'Fuera de servicio').length;

    this.chartData = {
      labels: ['Descanso', 'En ruta a la tienda', 'En Reparto', 'Fuera de servicio'],
      datasets: [{
        data: [resting, onRouteToShop, inDelivery, outOfService],
        backgroundColor: ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444'],
        hoverOffset: 15,
        borderWidth: 0
      }]
    };

    // Chart 2: driver assignment distribution
    const assigned = items.filter(t => t.assignedDriver).length;
    const unassigned = items.filter(t => !t.assignedDriver).length;

    this.assignedChartData = {
      labels: ['Conductor Asignado', 'Sin Conductor Asignado'],
      datasets: [{
        data: [assigned, unassigned],
        backgroundColor: ['#8b5cf6', '#94a3b8'], // Purple and grey to differentiate from the status chart colors
        hoverOffset: 15,
        borderWidth: 0
      }]
    };
  }

  private initMap(): void {
    const mapEl = document.getElementById('trucks-map');
    if (!mapEl) return;
    this.map = L.map('trucks-map', { zoomControl: false }).setView([40.4168, -3.7038], 6);
    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.map);
    this.map.attributionControl.setPrefix('Leaflet');
    setTimeout(() => {
      this.map?.invalidateSize();
    }, 100);
  }

  private renderTruckMarkers() {
    if (!this.map) return;
    this.markers.forEach(m => m.remove());
    this.markers = [];
    this.routePolylines.forEach(p => p.remove());
    this.routePolylines = [];
    const bounds = L.latLngBounds([]);
    let hasCoords = false;

    this.trucksPage.items.forEach(truck => {
      const pos = this.getEffectivePosition(truck);
      if (!pos) return;

      hasCoords = true;
      const currentStatus = this.getCurrentStatus(truck);
      let pinColor = '#ef4444';
      if (currentStatus === 'Descanso') pinColor = '#22c55e';
      else if (currentStatus === 'En ruta a la tienda') pinColor = '#3b82f6';
      else if (currentStatus === 'En Reparto') pinColor = '#f59e0b';

      const borderStyle = pos.source === 'gps'
        ? 'border: 3px solid #22c55e;'
        : 'border: 3px solid white;';
      const customIcon = L.divIcon({
        className: 'custom-pin',
        html: `<div style="background-color: ${pinColor}; width: 22px; height: 22px; border-radius: 50%; ${borderStyle} box-shadow: 0 4px 6px rgba(0,0,0,0.3);"></div>`,
        iconSize: [22, 22], iconAnchor: [11, 11]
      });

      const posLabel = pos.source === 'gps' ? '📍 GPS conductor' : '📍 Última posición guardada';
      const marker = L.marker([pos.lat, pos.lng], { icon: customIcon })
        .addTo(this.map!)
        .bindPopup(`<div class="text-center font-sans"><strong>${truck.plateNumber}</strong><br><span class="text-xs">${truck.assignedDriver?.name || 'Sin conductor'}</span><br><span class="text-xs text-slate-400">${posLabel}</span></div>`);
      this.markers.push(marker);
      bounds.extend([pos.lat, pos.lng]);

      // Draw route and destination marker for trucks in transit
      if (currentStatus === 'En ruta a la tienda' && truck.shopAddress?.latitude && truck.shopAddress?.longitude) {
        const shopIcon = L.icon({ iconUrl: '/shopIcon.png', iconSize: [35, 35], iconAnchor: [17, 35], popupAnchor: [0, -35] });
        const destMarker = L.marker([truck.shopAddress.latitude, truck.shopAddress.longitude], { icon: shopIcon })
          .addTo(this.map!)
          .bindPopup(`<strong>Tienda destino</strong><br><span class="text-xs">${truck.plateNumber}</span>`);
        this.markers.push(destMarker);
        bounds.extend([truck.shopAddress.latitude, truck.shopAddress.longitude]);
        this.drawRouteOnMap(pos.lat, pos.lng, truck.shopAddress.latitude, truck.shopAddress.longitude, '#3b82f6', marker);
      } else if (currentStatus === 'En Reparto' && truck.selectedOrderAddressLat && truck.selectedOrderAddressLng) {
        const destIcon = L.icon({ iconUrl: '/location-pointer.png', iconSize: [32, 32], iconAnchor: [16, 32], popupAnchor: [0, -32] });
        const destMarker = L.marker([truck.selectedOrderAddressLat, truck.selectedOrderAddressLng], { icon: destIcon })
          .addTo(this.map!)
          .bindPopup(`<strong>Destino de entrega</strong><br><span class="text-xs">${truck.plateNumber}</span>`);
        this.markers.push(destMarker);
        bounds.extend([truck.selectedOrderAddressLat, truck.selectedOrderAddressLng]);
        this.drawRouteOnMap(pos.lat, pos.lng, truck.selectedOrderAddressLat, truck.selectedOrderAddressLng, '#8b5cf6', marker);
      }
    });
    if (hasCoords) this.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 12 });
  }

  private drawRouteOnMap(fromLat: number, fromLng: number, toLat: number, toLng: number, color: string, marker: L.Marker) {
    this.locationService.getRoute(fromLat, fromLng, toLat, toLng).subscribe(route => {
      if (!route || !this.map) return;
      const latlngs: L.LatLngTuple[] = route.coordinates.map(([lng, lat]) => [lat, lng]);
      const polyline = L.polyline(latlngs, { color, weight: 5, opacity: 0.75 }).addTo(this.map);
      this.routePolylines.push(polyline);
      const eta = formatDuration(route.durationSeconds);
      const popupContent = marker.getPopup()?.getContent() || '';
      marker.getPopup()?.setContent(popupContent + `<br><span class="text-xs font-bold text-blue-600">⏱ ETA: ${eta}</span>`);
    });
  }

  private renderDriverMarkers() {
    if (!this.map) return;
    this.driverMarkers.forEach(m => m.remove());
    this.driverMarkers = [];

    const assignedUsernames = new Set(
      this.trucksPage.items
        .filter(t => t.assignedDriver)
        .map(t => t.assignedDriver!.username)
    );

    this.allDriverLocations
      .filter(dl => !assignedUsernames.has(dl.driverUsername))
      .forEach(dl => {
        if (!dl.address?.latitude || !dl.address?.longitude) return;
        const driverIcon = L.divIcon({
          className: 'custom-pin',
          html: `<div style="background-color: #8b5cf6; width: 18px; height: 18px; border-radius: 50%; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>`,
          iconSize: [18, 18], iconAnchor: [9, 9]
        });
        const marker = L.marker([dl.address.latitude, dl.address.longitude], { icon: driverIcon })
          .addTo(this.map!)
          .bindPopup(`<div class="text-center font-sans"><strong>${dl.driverName}</strong><br><span class="text-xs text-purple-500">👤 Conductor sin camión</span></div>`);
        this.driverMarkers.push(marker);
      });
  }

  getEffectivePosition(truck: Truck): { lat: number; lng: number; source: 'gps' | 'saved' } | null {
    if (truck.assignedDriver && truck.driverLocation?.address?.latitude && truck.driverLocation?.address?.longitude) {
      return { lat: truck.driverLocation.address.latitude, lng: truck.driverLocation.address.longitude, source: 'gps' };
    }
    if (truck.address?.latitude && truck.address?.longitude) {
      return { lat: truck.address.latitude, lng: truck.address.longitude, source: 'saved' };
    }
    return null;
  }

  locateTruckOnMap(truck: Truck) {
    this.selectedViewMode = 'map';
    this.onViewModeChange();
    setTimeout(() => {
      const pos = this.getEffectivePosition(truck);
      if (this.map && pos) {
        this.map.flyTo([pos.lat, pos.lng], 14, { duration: 1.5 });
      }
    }, 100);
  }

  getCurrentStatus(truck: Truck): string {
    if (!truck || !truck.history || truck.history.length === 0) return 'Disponible';
    return truck.history[truck.history.length - 1].status;
  }

  getStatusLabel(status: string): string {
    if (!status) return 'Desconocido';
    return status.charAt(0).toUpperCase() + status.slice(1);
  }

  protected readonly getTruckHistoryStatusTagInfo = getTruckHistoryStatusTagInfo;

  getLoadPercentage(active: number, max: number): number {
    return max === 0 ? 0 : Math.round((active / max) * 100);
  }

  deleteTruck(truckId: string) {
    this.confirmationService.confirm({
      message: '¿Estás seguro de eliminar el camión? Todos pedidos en reparto pasarán a estado Enviado.',
      header: 'Borrar camión',
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: {
        label: 'Cancelar',
        severity: 'secondary',
        outlined: true,
      },
      acceptButtonProps: {
        severity: 'danger',
        label: 'Borrar',
      },
      accept: () => {
        this.truckService.deleteTruck(truckId).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'El camión ha sido eliminado correctamente.' });
            this.loadTrucks();
          },
          error: () => {
            this.messageService.add({ severity: 'danger', summary: 'Error', detail: 'No se ha podido eliminar el camión.' });
          }
        })
      },
      reject: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Rejected',
          detail: 'You have rejected',
          life: 3000,
        });
      },
    });
  }

  openHistory(truck: Truck) {
    this.selectedTruck = truck;
    this.newHistoryStatus = this.getCurrentStatus(truck);
    this.newHistoryComment = '';
    this.displayHistoryDialog = true;
  }

  addHistoryComment() {
    if (!this.newHistoryComment.trim() || !this.selectedTruck) return;

    const truckId = this.selectedTruck.id;
    const newStatus = this.newHistoryStatus;
    const comment = this.newHistoryComment.trim();
    const previousStatus = this.getCurrentStatus(this.selectedTruck);

    this.truckService.commentAndOrUpdateTruckStatus(truckId, newStatus, comment).subscribe({
      next: (updatedTruck) => {
        this.selectedTruck = updatedTruck;

        const index = this.trucksPage.items.findIndex(t => t.id === truckId);
        if (index !== -1) {
          this.trucksPage.items[index] = updatedTruck;
        }
        this.newHistoryComment = '';

        // If status changed, then refresh the visual elements and KPIs
        if (newStatus !== previousStatus) {
          this.calculateKPIs(this.trucksPage.items);
          this.updateChartData(this.trucksPage.items);
          this.renderTruckMarkers();

          this.messageService.add({
            severity: 'success',
            summary: 'Estado Actualizado',
            detail: `Camión movido a ${newStatus}`
          });
        } else {
          this.messageService.add({
            severity: 'info',
            summary: 'Comentario añadido',
            detail: 'Se ha registrado el comentario en el historial.'
          });
        }
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo actualizar el historial del camión.'
        });
      }
    });
  }

  openAssignmentDialog(truck: Truck) {
    this.userService.getAvailableDrivers().subscribe({
      next: (availableDrivers) => {
        let finalDriversList = [...availableDrivers];

        if (truck.assignedDriver) {
          const isAlreadyIncluded = finalDriversList.some(d => d.id === truck.assignedDriver?.id);

          if (!isAlreadyIncluded) {
            finalDriversList.unshift(truck.assignedDriver);
          }
        }

        this.drivers = finalDriversList;
        this.currentTruckForAssignment = truck;
        this.selectedDriver = truck.assignedDriver ? this.drivers.find(d => d.id === truck.assignedDriver?.id) : undefined;
        this.displayAssignmentDialog = true;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los conductores.' });
      }
    });
  }

  cancelAssignment() {
    this.displayAssignmentDialog = false;
    this.currentTruckForAssignment = undefined;
    this.selectedDriver = undefined;
  }

  confirmAssignment(truckId: string) {
    const selectedDriver = this.selectedDriver;

    if (selectedDriver) {
      this.truckService.assignDriver(selectedDriver.id, truckId, true).subscribe({
        next: () => {
          const index = this.trucksPage.items.findIndex(t => t.id === truckId);

          if (index !== -1) {
            this.trucksPage.items[index] = {
              ...this.trucksPage.items[index],
              assignedDriver: selectedDriver
            };
          }
          this.calculateKPIs(this.trucksPage.items);
          this.updateChartData(this.trucksPage.items);
          this.renderTruckMarkers();
          this.cancelAssignment();
        },
        error: (err) => {
          console.error('Error al asignar conductor', err);
        }
      });
    }
  }

  unassignDriver(truckId: string) {
    this.truckService.assignDriver("-1", truckId, false).subscribe({
      next: () => {
        const index = this.trucksPage.items.findIndex(t => t.id === truckId);
        if (index !== -1) {
          this.trucksPage.items[index] = {
            ...this.trucksPage.items[index],
            assignedDriver: undefined
          };
        }

        this.messageService.add({
          severity: 'success',
          summary: 'Actualizado',
          detail: 'Conductor desasignado correctamente'
        });

        this.calculateKPIs(this.trucksPage.items);
        this.updateChartData(this.trucksPage.items);
        this.renderTruckMarkers();
        this.cancelAssignment();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo desasignar al conductor.'
        });
      }
    });
  }

  protected readonly formatAddress = formatAddress;
  protected readonly formatDuration = formatDuration;
}
