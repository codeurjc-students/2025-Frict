import {AfterViewInit, Component, DestroyRef, inject, OnInit, PLATFORM_ID, signal, ViewChild} from '@angular/core';
import {isPlatformBrowser, NgClass} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {catchError, debounceTime, filter, of, switchMap} from 'rxjs';

import {InputText} from 'primeng/inputtext';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {FileUpload} from 'primeng/fileupload';
import {MessageService} from 'primeng/api';
import {DomSanitizer} from '@angular/platform-browser';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {ShopService} from '../../../services/shop.service';
import {Shop} from '../../../models/shop.model';
import {LocalImage} from '../../../models/localImage.model';
import * as L from 'leaflet';
import {LocationService} from '../../../services/location.service';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';

@Component({
  selector: 'app-create-edit-shop',
  standalone: true,
  imports: [
    NgClass,
    Button,
    ReactiveFormsModule,
    InputText,
    InputNumber,
    FileUpload,
    FormsModule,
    RouterLink,
    LoadingScreenComponent,
    BreadcrumbReloadComponent
  ],
  templateUrl: './create-edit-shop.component.html',
  styleUrl: './create-edit-shop.component.css'
})
export class CreateEditShopComponent implements OnInit, AfterViewInit {
  private destroyRef = inject(DestroyRef);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private messageService = inject(MessageService);
  private route = inject(ActivatedRoute);
  private shopService = inject(ShopService);
  private sanitizer = inject(DomSanitizer);
  private locationService = inject(LocationService);
  private breadcrumbService = inject(BreadcrumbService);
  private platformId = inject(PLATFORM_ID);

  constructor() {

    this.shopForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      referenceCode: [{ value: '', disabled: true }],
      assignedBudget: [0, [Validators.required, Validators.min(0.01)]],
      maxCapacity: [0, [Validators.required, Validators.min(0)]],
      // Group address
      address: this.fb.group({
        alias: ['', []],
        street: ['', Validators.required],
        number: ['', Validators.required],
        floor: ['', []],
        postalCode: ['', Validators.required],
        city: ['', Validators.required],
        country: ['', Validators.required],
        latitude: [0, [Validators.required]],
        longitude: [0, [Validators.required]]
      })
    });
  }

  @ViewChild('fileUploader') fileUploader: FileUpload | undefined;

  shopId = signal<string | null>(null);
  shop = signal<Shop | null>(null);

  // Unique image logic
  existingImage = signal<string | null>(null);
  newImage = signal<LocalImage | null>(null);

  protected readonly MAX_SIZE = 5000000;
  shopForm: FormGroup;

  loading: boolean = true;
  error: boolean = false;

  // Leaflet
  private map: L.Map | undefined;
  private marker: L.Marker | undefined;
  private lastAddressCheck: string = '';
  private isGeocodingActive: boolean = false;

  ngOnInit() {
    this.shopId.set(this.route.snapshot.paramMap.get('id'));
    this.setupAddressListener();
    this.setupNameAliasSync();
    this.loadData();
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        if (!this.loading) this.initMap();
      }, 10);
    }
  }

  public reloadAll() {
    this.loading = true;
    this.error = false;

    // 1. Clear Leaflet map
    if (this.map) {
      this.map.remove();
      this.map = undefined;
      this.marker = undefined;
    }

    // 2. Clear TS memory in Create mode
    if (!this.shopId()) {
      this.shopForm.reset({
        assignedBudget: 0,
        maxCapacity: 0,
        address: {
          latitude: 0,
          longitude: 0
        }
      });

      this.newImage.set(null);
      this.existingImage.set(null);
      if (this.fileUploader) {
        this.fileUploader.clear();
      }
    }
    this.loadData();
  }

  isInvalid(controlPath: string): boolean {
    const ctrl = this.shopForm.get(controlPath);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  private setupNameAliasSync() {
    this.shopForm.get('name')?.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(name => {
      const aliasControl = this.shopForm.get('address.alias');
      const currentAlias: string = aliasControl?.value ?? '';
      if (!currentAlias || currentAlias.startsWith('Ubicación de ')) {
        aliasControl?.setValue(`Ubicación de ${name}`, { emitEvent: false });
      }
    });
  }

  private setupAddressListener() {
    this.shopForm.get('address')?.valueChanges.pipe(
      filter(() => this.isGeocodingActive),
      debounceTime(1000),

      // Detect changes in all fields but the alias or the number
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
        this.messageService.add({
          severity: 'info',
          summary: 'Ubicación actualizada',
          detail: 'Se ha movido el marcador según la dirección ingresada.'
        });
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Dirección no encontrada',
          detail: 'No se encontraron coordenadas para la dirección indicada.'
        });
      }
    });
  }


  loadData() {
    // The geocoding lock starts inactive
    this.isGeocodingActive = false;

    const shopId = this.shopId();
    const currentUrl = this.router.url;

    if (shopId) {
      this.shopService.getShopById(shopId).subscribe({
        next: (shop) => {
          this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
            { label: 'Gestor de Tiendas', routerLink: '/admin/shops' },
            { label: shop.name, routerLink: `/admin/shop/${shop.id}` }
          ]);

          this.shop.set(shop);
          this.existingImage.set(shop.imageInfo.imageUrl);

          // EmitEvent false to avoid trigger geocoding while loading
          this.shopForm.patchValue({
            name: shop.name,
            referenceCode: shop.referenceCode,
            address: shop.address,
            assignedBudget: shop.assignedBudget,
            maxCapacity: shop.maxCapacity
          }, { emitEvent: false });

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

      this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
        { label: 'Gestor de Tiendas', routerLink: '/admin/shops' }
      ]);

      this.loading = false;
      // Also wait for load finishing to start geocoding
      setTimeout(() => {
        this.syncAddressMemory();
        this.isGeocodingActive = true;
        this.initMap();
      }, 100);
    }
  }

  private syncAddressMemory() {
    const updated = this.shopForm.get('address')?.getRawValue();
    this.lastAddressCheck = `${updated.street}|${updated.number}|${updated.city}|${updated.postalCode}|${updated.country}`;
  }

  // --- LEAFLET MAP LOGIC ---
  private initMap(): void {
    if (this.map) return;

    const container = document.getElementById('map');
    if (!container) return;

    const lat = this.shopForm.get('address.latitude')?.value || 40.4168;
    const lng = this.shopForm.get('address.longitude')?.value || -3.7038;
    const zoom = this.shopId() ? 15 : 6;

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

    if (this.shopId()) {
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
    this.shopForm.patchValue({
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
          this.shopForm.get('address')?.patchValue(addressData, { emitEvent: false });
          this.syncAddressMemory();

          if (addressData.number) {
            this.messageService.add({
              severity: 'info',
              summary: 'Dirección Exacta',
              detail: `Detectado nº ${addressData.number}`
            });
          } else {
            this.messageService.add({
              severity: 'info',
              summary: 'Zona detectada',
              detail: 'Ubicación aproximada (sin número exacto)'
            });
          }

          // Open the lock giving a time for Angular to end loading the data
          setTimeout(() => {
            this.isGeocodingActive = true;
          }, 50);
        } else {
          this.isGeocodingActive = true;
          this.messageService.add({
            severity: 'warn',
            summary: 'Dirección no encontrada',
            detail: 'No se encontró ninguna dirección para la ubicación indicada.'
          });
        }
      },
      error: (err) => {
        console.error('Error en geocodificación inversa:', err);

        // If the API fails, then open the lock again
        this.isGeocodingActive = true;

        this.messageService.add({
          severity: 'warn',
          summary: 'Aviso',
          detail: 'No se pudo recuperar la dirección automática.'
        });
      }
    });
  }

  // --- IMAGE LOGIC (SINGLE) ---
  onFileSelect(event: any) {
    const file = event.files[0];
    if (file && file.size <= this.MAX_SIZE) {
      const preview = this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(file));

      // Replace previous images
      this.newImage.set({
        file: file,
        previewUrl: preview
      });
      this.existingImage.set(null);
    }
  }

  onFileRemove() {
    this.newImage.set(null);
  }

  removeImage() {
    this.newImage.set(null);
    this.existingImage.set(null);
    if (this.fileUploader) {
      this.fileUploader.clear();
    }
  }

  restoreImage() {
    const oldImage = this.shop()?.imageInfo.imageUrl;
    if (oldImage){
      this.existingImage.set(oldImage);
    }
  }

  // --- SUBMIT ---
  onSubmit() {
    if (this.shopForm.invalid) {
      this.shopForm.markAllAsTouched();
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos requeridos.' });
      return;
    }

    const formValue = this.shopForm.getRawValue();
    const shopData: Shop = {
      ...formValue
    };

    const imageFile = this.newImage()?.file;
    const shopId = this.shopId();

    if(shopId){
      this.shopService.updateShop(shopId, shopData).subscribe({
        next: (shop) => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Tienda actualizada.' });
          this.updateShopImage(shop.id, imageFile);

          this.router.navigate(['/admin/shops']);
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al actualizar.' })
      });
    } else {
      this.shopService.createShop(shopData).subscribe({
        next: (shop) => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Tienda creada.' });

          // Llamada incondicional
          this.updateShopImage(shop.id, imageFile);

          this.router.navigate(['/admin/shops']);
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al crear.' })
      });
    }
  }

  protected updateShopImage(id: string, image?: File) {
    this.shopService.updateShopImage(id, image).subscribe({
      next: (shop) => {
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Imagen de tienda ${shop.name} actualizada.` });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se ha podido actualizar la imagen de la tienda.' });
      }
    })
  }

}
