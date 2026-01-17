import {Injectable} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {AuthService} from '../services/auth.service';

export type IconType = 'client' | 'admin';

@Injectable({
  providedIn: 'root'
})
export class UiService {
  constructor(private titleService: Title,
              private authService: AuthService) {
  }


  public readonly CLIENT_APP_NAME = 'MiTienda';
  public readonly ADMIN_APP_NAME = 'Frict';
  public readonly APP_USER_LOGO = '/shopLogo.png';
  public readonly APP_ADMIN_LOGO = '/frictLogo.png';
  private readonly PREDEFINED_ICONS: Record<string, string> = {
    client: '/shopLogo.png',
    admin: '/frictLogo.png',
  };


  // Update tab name
  setPageTitle(pageName: string) {
    if(this.authService.isManager() || this.authService.isAdmin()){
      this.titleService.setTitle(`${pageName} - ${this.ADMIN_APP_NAME}`);
    }
    else this.titleService.setTitle(`${pageName} - ${this.CLIENT_APP_NAME}`);
  }

  setTitle(fullTitle: string) {
    this.titleService.setTitle(fullTitle);
  }

  addToTitle(textToAppend: string, separator: string = ' - ') {
    const currentTitle = this.titleService.getTitle();
    if (!currentTitle.includes(textToAppend)) {
      this.titleService.setTitle(`${currentTitle}${separator}${textToAppend}`);
    }
  }

  prependToTitle(textToPrepend: string, separator: string = ' - ') {
    const currentTitle = this.titleService.getTitle();
    this.titleService.setTitle(`${textToPrepend}${separator}${currentTitle}`);
  }

  //Works either with 'client'-like literals or with a route '/photo.png'
  setFavicon(keyOrUrl: string) {
    const finalUrl = this.PREDEFINED_ICONS[keyOrUrl] || keyOrUrl;
    this.updateFaviconTag(finalUrl);
  }

  private updateFaviconTag(url: string) {
    const link: HTMLLinkElement = document.querySelector("link[rel~='icon']") || document.createElement('link');
    link.rel = 'icon';
    link.href = url;
    if (!document.head.contains(link)) {
      document.head.appendChild(link);
    }
  }

  //Reset
  resetDefault() {
    this.setTitle(this.CLIENT_APP_NAME);
    if(this.authService.isAdmin()){
      this.setFavicon(this.APP_ADMIN_LOGO);
    }
    else{
      this.setFavicon(this.APP_USER_LOGO);
    }
  }
}
