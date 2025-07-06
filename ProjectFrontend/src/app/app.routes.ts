import { Routes } from '@angular/router';
import {IndexComponent} from './index/index.component';
import {ProductInfoComponent} from './product-info/product-info.component';

export const routes: Routes = [
  {path: 'index', component: IndexComponent}, //La ruta '' será la que se muestre por defecto en el router cuando se inicia la aplicación. Si no se define, el router no mostrará nada dentro de su componente
  {path: 'product-info', component: ProductInfoComponent}
];
