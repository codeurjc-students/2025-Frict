import { Routes } from '@angular/router';
import {LoginComponent} from './components/common/login/login.component';
import {ClientHomeComponent} from './components/client/client-home/client-home.component';
import {SignupComponent} from './components/common/signup/signup.component';
import {ErrorComponent} from './components/common/error/error.component';
import {ProductInfoComponent} from './components/client/product-info/product-info.component';


const icons: Record<'client' | 'admin', string> = {
  client: '/shopLogo.png',
  admin: '/frictLogo.png'
};

export const routes: Routes = [
  //Client side routes (anon users, registered users and store managers)
  { path: '', component: ClientHomeComponent, data: { title: 'Inicio - MiTienda', icon: icons.client } },
  { path: 'product', component: ProductInfoComponent, data: { title: 'Producto - MiTienda', icon: icons.client } },


  //Common routes
  { path: 'login', component: LoginComponent, data: { title: 'Iniciar sesión - MiTienda', icon: icons.client } },
  { path: 'signup', component: SignupComponent, data: { title: 'Registro - MiTienda', icon: icons.client } },
  { path: 'error', component: ErrorComponent, data: { title: 'Error', icon: icons.client } },

  //Inexistent routes
  {path: '**', redirectTo: ''},
];
