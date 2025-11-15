import {Component, OnInit} from '@angular/core';
import {FooterComponent} from '../../common/footer/footer.component';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {GalleriaModule} from 'primeng/galleria';
import {carouselResponsiveOptions, galleryResponsiveOptions} from '../../../app.config';
import {Product} from '../../../models/product.model';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {Select} from 'primeng/select';
import {FormsModule} from '@angular/forms';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {Carousel} from 'primeng/carousel';
import {LoadingComponent} from '../../common/loading/loading.component';
import {ProductCardComponent} from '../product-card/product-card.component';
import {RouterLink} from '@angular/router';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';


@Component({
  selector: 'app-product-info',
  standalone: true,
  imports: [
    FooterComponent,
    NavbarComponent,
    GalleriaModule,
    Tabs,
    TabList,
    TabPanels,
    Tab,
    TabPanel,
    Select,
    FormsModule,
    InputNumber,
    Button,
    Carousel,
    LoadingComponent,
    ProductCardComponent,
    RouterLink,
    Breadcrumb
  ],
  templateUrl: './product-info.component.html'
})
export class ProductInfoComponent implements OnInit {

  images: any[] = [];
  product!: Product;

  constructor() {
    this.images = [
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria1.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria1s.jpg'
      },
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria2.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria2s.jpg'
      },
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria3.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria3s.jpg'
      },
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria4.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria4s.jpg'
      },
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria5.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria5s.jpg'
      },
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria6.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria6s.jpg'
      },
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria7.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria7s.jpg'
      },
      {
        itemImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria8.jpg',
        thumbnailImageSrc: 'https://primefaces.org/cdn/primeng/images/galleria/galleria8s.jpg'
      }
    ];

  }

  protected readonly galleryResponsiveOptions = galleryResponsiveOptions;
  protected readonly carouselResponsiveOptions = carouselResponsiveOptions;

  protected productModels: any[] = [
    { name: 'Modelo 1', code: 'M1' },
    { name: 'Modelo 2', code: 'M2' },
    { name: 'Modelo 3', code: 'M3' },
    { name: 'Modelo 4', code: 'M4' },
    { name: 'Modelo 5', code: 'M5' },
  ];
  protected selectedModel: any = this.productModels[0];
  protected quantity: number = 1;

  protected loading: boolean = true;
  protected error: boolean = false;
  protected categoryProducts: Product[] = [];

  previousPages: MenuItem[] | undefined;
  homePage: MenuItem | undefined;

  ngOnInit() {
    this.previousPages = [{ icon: 'pi pi-home', route: '/installation' }, { label: 'Buscar' }, { label: 'Ordenadores', route: '/inputtext' }];
  }
}
