import { Component, OnInit, AfterViewInit, signal, Inject, PLATFORM_ID, DestroyRef, inject } from '@angular/core';
import { NgIf, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, switchMap, filter, catchError, of } from 'rxjs';

import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import * as L from 'leaflet';
import { LocationService } from '../../../services/location.service';

import { Truck } from '../../../models/truck.model';
import { TruckService } from '../../../services/truck.service';
import { Shop } from '../../../models/shop.model';
import { ShopService } from '../../../services/shop.service';

@Component({
  selector: 'app-create-edit-truck',
  standalone: true,
  imports: [
    Button,
    ReactiveFormsModule,
    InputText,
    InputNumber,
    Select,
    NgIf,
    FormsModule,
    RouterLink,
    LoadingScreenComponent
  ],
  templateUrl: './create-edit-truck.component.html'
})
export class CreateEditTruckComponent implements OnInit, AfterViewInit {
  private destroyRef = inject(DestroyRef);

  constructor(private fb: FormBuilder,
              private router: Router,
              private messageService: MessageService,
              private route: ActivatedRoute,
              private truckService: TruckService,
              private locationService: LocationService,
              private shopService: ShopService,
              @Inject(PLATFORM_ID) private platformId: Object) {

    this.truckForm = this.fb.group({
      plateNumber: ['', [Validators.required, Validators.minLength(4)]],
      referenceCode: [{ value: '', disabled: true }],
      maxOrderCapacity: [10, [Validators.required, Validators.min(1)]],
      shopId: [''], // Campo opcional como solicitaste

      address: this.fb.group({
        alias: ['Última ubicación conocida', []],
        street: ['', Validators.required],
        number: ['', Validators.required],
        floor: ['', []],
        postalCode: ['', Validators.required],
        city: ['', Validators.required],
        country: ['España', Validators.required],
        latitude: [0, [Validators.required]],
        longitude: [0, [Validators.required]]
      })
    });
  }

  truckId = signal<string | null>(null);
  truck = signal<Truck | null>(null);

  truckForm: FormGroup;

  loading: boolean = true;
  error: boolean = false;

  // Variables para la carga perezosa de Tiendas
  availableShops: Shop[] = [];
  shopsLoaded: boolean = false;
  loadingShops: boolean = false;

  // Leaflet
  private map: L.Map | undefined;
  private marker: L.Marker | undefined;
  private lastAddressCheck: string = '';
  private isGeocodingActive: boolean = false;

  ngOnInit() {
    this.truckId.set(this.route.snapshot.paramMap.get('id'));
    this.setupAddressListener();
    this.loadData();
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        if (!this.loading) this.initMap();
      }, 10);
    }
  }

  private setupAddressListener() {
    this.truckForm.get('address')?.valueChanges.pipe(
      filter(() => this.isGeocodingActive),
      debounceTime(1000),
      filter(address => {
        const currentCheck = `${address.street}|${address.number}|${address.city}|${address.postalCode}|${address.country}`;
        if (this.lastAddressCheck === currentCheck) {
          return false;
        }
        this.lastAddressCheck = currentCheck;
        return !!(address.street && address.city);
      }),
      switchMap(address =>
        this.locationService.getCoordinatesFromAddress(address).pipe(
          catchError(() => of(null))
        )
      ),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(coords => {
      if (coords) {
        this.updateFormCoordinates(coords.latitude, coords.longitude);
        this.updateMarker(coords.latitude, coords.longitude);
        if (this.map) this.map.setView([coords.latitude, coords.longitude], 16);
        this.messageService.add({ severity: 'info', summary: 'Ubicación actualizada', detail: 'Se ha movido el marcador según la dirección ingresada.' });
      }
    });
  }

  loadData() {
    this.isGeocodingActive = false;

    const currentId = this.truckId();
    if (currentId) {
      this.truckService.getTruckById(currentId).subscribe({
        next: (truck) => {
          this.truck.set(truck);

          this.truckForm.patchValue({
            plateNumber: truck.plateNumber,
            referenceCode: truck.referenceCode,
            maxOrderCapacity: truck.maxOrderCapacity,
            shopId: truck.shopId,
            address: truck.address
          }, { emitEvent: false });

          // If the truck belongs to a shop, load it to the available shops list
          if (truck.shopId) {
            this.shopService.getShopById(truck.shopId).subscribe({
              next: (shop) => {
                this.availableShops = [shop]; // Store temporarily
              }
            });
          }

          this.loading = false;
          setTimeout(() => {
            this.syncAddressMemory();
            this.isGeocodingActive = true;
            this.initMap();
          }, 100);
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    } else {
      this.loading = false;
      setTimeout(() => {
        this.syncAddressMemory();
        this.isGeocodingActive = true;
        this.initMap();
      }, 100);
    }
  }

  // Loaded when clicking over p-select element (lazy loading)
  loadAvailableShops() {
    if (this.shopsLoaded) return;
    this.loadingShops = true;

    this.shopService.getAllShopsList().subscribe({
      next: (shops) => {
        const newShops = [...shops];
        if (this.availableShops.length === 1) {
          const currentShop = this.availableShops[0];
          if (!newShops.some(s => s.id === currentShop.id)) {
            newShops.unshift(currentShop);
          }
        }

        this.availableShops = newShops;
        this.shopsLoaded = true;
        this.loadingShops = false;
      },
      error: () => {
        this.loadingShops = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar las tiendas disponibles.' });
      }
    });
  }

  private syncAddressMemory() {
    const updated = this.truckForm.get('address')?.getRawValue();
    this.lastAddressCheck = `${updated.street}|${updated.number}|${updated.city}|${updated.postalCode}|${updated.country}`;
  }

  private initMap(): void {
    if (this.map) return;

    const container = document.getElementById('map');
    if (!container) return;

    const lat = this.truckForm.get('address.latitude')?.value || 40.4168;
    const lng = this.truckForm.get('address.longitude')?.value || -3.7038;
    const zoom = this.truckId() ? 15 : 6;

    this.map = L.map('map').setView([lat, lng], zoom);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);

    this.map.attributionControl.setPrefix('Leaflet');

    const iconDefault = L.icon({
      iconUrl: './location-pointer.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;

    if (this.truckId()) {
      this.updateMarker(lat, lng);
    }

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.updateMarker(e.latlng.lat, e.latlng.lng);
      this.updateFormCoordinates(e.latlng.lat, e.latlng.lng);
      this.getAddressFromCoordinates(e.latlng.lat, e.latlng.lng);
    });
  }

  private updateMarker(lat: number, lng: number) {
    if (this.marker) {
      this.marker.setLatLng([lat, lng]);
    } else {
      this.marker = L.marker([lat, lng]).addTo(this.map!);
    }
  }

  private updateFormCoordinates(lat: number, lng: number) {
    this.truckForm.patchValue({
      address: {
        latitude: lat,
        longitude: lng
      }
    }, { emitEvent: false });
  }

  private getAddressFromCoordinates(lat: number, lng: number) {
    this.isGeocodingActive = false;
    this.locationService.getAddressFromCoordinates(lat, lng).subscribe({
      next: (addressData) => {
        if (addressData) {
          this.truckForm.get('address')?.patchValue(addressData, { emitEvent: false });
          this.syncAddressMemory();

          if (addressData.number) {
            this.messageService.add({ severity: 'info', summary: 'Dirección Exacta', detail: `Detectado nº ${addressData.number}` });
          } else {
            this.messageService.add({ severity: 'info', summary: 'Zona detectada', detail: 'Ubicación aproximada (sin número exacto)' });
          }

          setTimeout(() => { this.isGeocodingActive = true; }, 50);
        } else {
          this.isGeocodingActive = true;
        }
      },
      error: (err) => {
        console.error('Error en geocodificación inversa:', err);
        this.isGeocodingActive = true;
        this.messageService.add({ severity: 'warn', summary: 'Aviso', detail: 'No se pudo recuperar la dirección automática.' });
      }
    });
  }

  onSubmit() {
    if (this.truckForm.invalid) {
      this.truckForm.markAllAsTouched();
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos requeridos correctamente.' });
      return;
    }

    const formValue = this.truckForm.getRawValue();
    const truckData = {
      ...formValue
    };

    const currentId = this.truckId();

    if(currentId){
      this.truckService.updateTruck(currentId, truckData).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Camión actualizado correctamente.' });
          this.router.navigate(['/admin/trucks']);
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al actualizar el camión.' })
      });
    } else {
      this.truckService.createTruck(truckData).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Nuevo camión registrado.' });
          this.router.navigate(['/admin/trucks']);
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al registrar el camión.' })
      });
    }
  }
}
