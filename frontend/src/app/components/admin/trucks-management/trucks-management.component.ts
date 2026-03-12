import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import * as L from 'leaflet';

import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { User } from '../../../models/user.model';
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
import {ConfirmationService, MessageService} from 'primeng/api';
import {Alert} from '../../../utils/ui.service';

import { Tooltip } from 'primeng/tooltip';
import { Textarea } from 'primeng/textarea';
import { Truck } from '../../../models/truck.model';
import { TruckService } from '../../../services/truck.service';
import { formatAddress } from '../../../utils/textFormat.util';
import { UserService } from '../../../services/user.service';
import {Select} from 'primeng/select';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-trucks-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, LoadingScreenComponent, Button, SelectButton, UIChart, InputGroup, InputGroupAddon, InputText, TableModule, Avatar, Tag, ProgressBar, Paginator, Dialog, Select, Tooltip, Textarea, RouterLink
  ],
  templateUrl: './trucks-management.component.html'
})
export class TrucksManagementComponent implements OnInit, OnDestroy {

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
    { label: 'Distribución', value: 'chart', icon: 'pi pi-chart-pie' },
    { label: 'Notificaciones', value: 'alerts', icon: 'pi pi-bell' }
  ];
  selectedViewMode: string = 'map';

  chartData: any = { labels: [], datasets: [] };
  assignedChartData: any = { labels: [], datasets: [] };
  chartOptions: any;

  // Diálogo de Edición
  selectedTruck: Truck | null = null;

  // Diálogo de Historial
  displayHistoryDialog: boolean = false;
  newHistoryStatus: string = '';
  newHistoryComment: string = '';

  // --- NUEVAS VARIABLES PARA ASIGNACIÓN DE CONDUCTOR ---
  displayAssignmentDialog: boolean = false;
  drivers: User[] = [];
  selectedDriver: User | undefined = undefined;
  currentTruckForAssignment: Truck | undefined = undefined;

  private map: L.Map | undefined;
  private markers: L.Marker[] = [];

  statusOptions = [
    { label: 'Disponible', value: 'Disponible' },
    { label: 'En Ruta / Reparto', value: 'En ruta' },
    { label: 'En Mantenimiento', value: 'En mantenimiento' },
    { label: 'Fuera de Servicio', value: 'Fuera de servicio' }
  ];

  constructor(private truckService: TruckService,
              private messageService: MessageService,
              private userService: UserService,
              private confirmationService: ConfirmationService) {}

  ngOnInit() {
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
    this.onRouteTrucks = items.filter(t => this.getCurrentStatus(t)?.toLowerCase().includes('ruta') || this.getCurrentStatus(t)?.toLowerCase().includes('reparto')).length;
    this.noDriverTrucks = items.filter(t => !t.assignedDriver).length;
    this.maintenanceTrucks = items.filter(t => this.getCurrentStatus(t) === 'En mantenimiento').length;
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
    // --- Gráfica 1: Estados Originales ---
    const available = items.filter(t => this.getCurrentStatus(t) === 'Disponible').length;
    const onRoute = items.filter(t => this.getCurrentStatus(t)?.toLowerCase().includes('ruta') || this.getCurrentStatus(t)?.toLowerCase().includes('reparto')).length;
    const maintenance = items.filter(t => this.getCurrentStatus(t) === 'En mantenimiento').length;
    const outOfService = items.filter(t => this.getCurrentStatus(t) === 'Fuera de servicio').length;

    this.chartData = {
      labels: ['Disponible', 'En Ruta', 'Mantenimiento', 'Inactivo'],
      datasets: [{
        data: [available, onRoute, maintenance, outOfService],
        backgroundColor: ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444'],
        hoverOffset: 15,
        borderWidth: 0
      }]
    };

    // --- Gráfica 2: Asignaciones (NUEVO) ---
    const assigned = items.filter(t => t.assignedDriver).length;
    const unassigned = items.filter(t => !t.assignedDriver).length;

    this.assignedChartData = {
      labels: ['Conductor Asignado', 'Sin Conductor Asignado'],
      datasets: [{
        data: [assigned, unassigned],
        backgroundColor: ['#8b5cf6', '#94a3b8'], // Morado y Gris para diferenciarse de los estados
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
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OpenStreetMap', maxZoom: 19 }).addTo(this.map);
    this.map.attributionControl.setPrefix('');
  }

  private renderTruckMarkers() {
    if (!this.map) return;
    this.markers.forEach(m => m.remove());
    this.markers = [];
    const bounds = L.latLngBounds([]);
    let hasCoords = false;

    this.trucksPage.items.forEach(truck => {
      if (truck.address?.latitude && truck.address?.longitude) {
        hasCoords = true;
        const currentStatus = this.getCurrentStatus(truck)?.toLowerCase();
        let pinColor = '#ef4444';

        if (currentStatus === 'disponible') pinColor = '#22c55e';
        else if (currentStatus.includes('ruta') || currentStatus.includes('reparto')) pinColor = '#3b82f6';
        else if (currentStatus === 'en mantenimiento') pinColor = '#f59e0b';

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

  locateTruckOnMap(truck: Truck) {
    this.selectedViewMode = 'map';
    this.onViewModeChange();
    setTimeout(() => {
      if (this.map && truck.address?.latitude && truck.address?.longitude) {
        this.map.flyTo([truck.address.latitude, truck.address.longitude], 14, { duration: 1.5 });
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

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    const s = status?.toLowerCase() || '';
    if (s === 'disponible') return 'success';
    if (s.includes('ruta') || s.includes('reparto')) return 'info';
    if (s === 'en mantenimiento') return 'warn';
    if (s === 'fuera de servicio') return 'danger';
    return 'secondary';
  }

  getIconForStatus(status: string): string {
    const s = status?.toLowerCase() || '';
    if (s === 'disponible') return 'pi pi-check-circle';
    if (s.includes('ruta') || s.includes('reparto')) return 'pi pi-send';
    if (s === 'en mantenimiento') return 'pi pi-wrench';
    if (s === 'fuera de servicio') return 'pi pi-times-circle';
    return 'pi pi-info-circle';
  }

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
}
