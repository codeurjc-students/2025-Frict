import { Routes } from '@angular/router';
import {LoginComponent} from './components/common/login/login.component';
import {ProfileComponent} from './components/common/profile/profile.component';
import {ShopsComponent} from './components/admin/shops/shops.component';
import {DeliveryComponent} from './components/admin/delivery/delivery.component';
import {ReportsComponent} from './components/admin/reports/reports.component';
import {UsersComponent} from './components/admin/users/users.component';
import {ClientHomeComponent} from './components/client/client-home/client-home.component';
import {ErrorComponent} from './components/common/error/error.component';

export const routes: Routes = [

  //Rutas del lado del cliente
  {path: '', component: ClientHomeComponent}, //La ruta '' será la que se muestre por defecto en el router cuando se inicia la aplicación. Si no se define, el router no mostrará nada dentro de su componente

  //Rutas del lado del administrador
  {path: 'profile', component: ProfileComponent},
  {path: 'shops', component: ShopsComponent},
  {path: 'delivery', component: DeliveryComponent},
  {path: 'reports', component: ReportsComponent},
  {path: 'users', component: UsersComponent},

  //Rutas de login y registro
  {path: 'login', component: LoginComponent},

  //Ruta de error
  {path: 'error', component: ErrorComponent},

  //Ruta para redirigir a login si la ruta no existe
  {path: '**', redirectTo: 'login'},

];
