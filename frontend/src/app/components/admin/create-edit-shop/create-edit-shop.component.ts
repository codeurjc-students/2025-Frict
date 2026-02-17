import {Component, OnInit, AfterViewInit, signal, Inject, PLATFORM_ID, ViewChild} from '@angular/core';
import { NgIf, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { FileUpload } from 'primeng/fileupload';
import { MessageService } from 'primeng/api';
import { DomSanitizer } from '@angular/platform-browser';
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { ShopService } from '../../../services/shop.service';
import { Shop } from '../../../models/shop.model';
import { LocalImage } from '../../../models/localImage.model';
import * as L from 'leaflet';

@Component({
  selector: 'app-create-edit-shop',
  standalone: true,
  imports: [
    Button,
    ReactiveFormsModule,
    InputText,
    InputNumber,
    FileUpload,
    NgIf,
    FormsModule,
    RouterLink,
    LoadingScreenComponent
  ],
  templateUrl: './create-edit-shop.component.html',
  styleUrl: './create-edit-shop.component.css'
})
export class CreateEditShopComponent implements OnInit, AfterViewInit {

  constructor(private fb: FormBuilder,
              private router: Router,
              private messageService: MessageService,
              private route: ActivatedRoute,
              private shopService: ShopService,
              private sanitizer: DomSanitizer,
              private http: HttpClient,
              @Inject(PLATFORM_ID) private platformId: Object) {

    this.shopForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      referenceCode: [{ value: '', disabled: true }],
      latitude: [0, [Validators.required]],
      longitude: [0, [Validators.required]],
      // Group address
      address: this.fb.group({
        alias: ['', []],
        street: ['', Validators.required],
        number: ['', Validators.required],
        floor: ['', []],
        postalCode: ['', Validators.required],
        city: ['', Validators.required],
        country: ['España', Validators.required]
      })
    });
  }

  @ViewChild('fileUploader') fileUploader: FileUpload | undefined;

  shopId = signal<string | null>(null);
  shop = signal<Shop | null>(null);

  // Lógica para imagen única
  existingImage = signal<string | null>(null);
  newImage = signal<LocalImage | null>(null);

  protected readonly MAX_SIZE = 5000000;
  shopForm: FormGroup;

  loading: boolean = true;
  error: boolean = false;

  // Leaflet
  private map: L.Map | undefined;
  private marker: L.Marker | undefined;

  ngOnInit() {
    this.shopId.set(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        if (!this.loading) this.initMap();
      }, 10);
    }
  }

  loadData() {
    const shopId = this.shopId();
    if (shopId) {
      this.shopService.getShopById(shopId).subscribe({
        next: (shop) => {
          console.log(shop);
          this.shop.set(shop);
          this.existingImage.set(shop.imageInfo.imageUrl);
          this.shopForm.patchValue({
            name: shop.name,
            referenceCode: shop.referenceCode,
            latitude: shop.latitude,
            longitude: shop.longitude,
            address: shop.address
          });

          this.loading = false;
          setTimeout(() => {
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
      // Wait for initMap in creation mode
      setTimeout(() => this.initMap(), 100);
    }
  }

  // --- LEAFLET MAP LOGIC ---
  private initMap(): void {
    if (this.map) return;

    const container = document.getElementById('map');
    if (!container) return;

    const lat = this.shopForm.get('latitude')?.value || 40.4168;
    const lng = this.shopForm.get('longitude')?.value || -3.7038;
    const zoom = this.shopId() ? 15 : 6;

    this.map = L.map('map').setView([lat, lng], zoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

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
    this.shopForm.patchValue({ latitude: lat, longitude: lng });
  }

  // --- REVERSE GEOCODING ---
  private getAddressFromCoordinates(lat: number, lng: number) {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`;

    this.http.get<any>(url).subscribe({
      next: (data) => {
        if(data && data.address) {
          const addr = data.address;

          const road = addr.road || addr.pedestrian || addr.street || '';
          const number = addr.house_number || '';

          this.shopForm.get('address')?.patchValue({
            street: road,
            number: number,
            city: addr.city || addr.town || addr.village || '',
            postalCode: addr.postcode || '',
            country: addr.country || ''
          });

          if (number) {
            this.messageService.add({severity: 'info', summary: 'Dirección Exacta', detail: `Detectado nº ${number}`});
          } else {
            this.messageService.add({severity: 'info', summary: 'Zona detectada', detail: 'Ubicación aproximada (sin número exacto)'});
          }
        }
      },
      error: () => {
        console.warn('No se pudo obtener la dirección');
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

  onFileRemove(event: any) {
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
          if (imageFile){
            this.updateShopImage(shop.id, imageFile);
          }
          this.router.navigate(['/admin/shops']);
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al actualizar.' })
      });
    } else {
      this.shopService.createShop(shopData).subscribe({
        next: (shop) => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Tienda creada.' });
          if (imageFile){
            this.updateShopImage(shop.id, imageFile);
          }
          this.router.navigate(['/admin/shops']);
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al crear.' })
      });
    }
  }

  protected updateShopImage(id: string, image: File) {
    const selectedImage = this.newImage();
    if (selectedImage){
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

}
