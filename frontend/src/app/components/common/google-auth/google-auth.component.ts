import {AfterViewInit, Component, ElementRef, NgZone, ViewChild, viewChild} from '@angular/core';
import {AuthService} from '../../../services/auth.service';
import {Router} from '@angular/router';
import {environment} from '../../../../environments/environment';

declare const google: any;

@Component({
  selector: 'app-google-auth',
  standalone: true,
  imports: [],
  templateUrl: './google-auth.component.html',
  styleUrl: './google-auth.component.css'
})
export class GoogleAuthComponent implements AfterViewInit {
  @ViewChild('googleBtn', { static: true }) googleBtn!: ElementRef;

  constructor(private authService: AuthService,
              private router: Router,
              private ngZone: NgZone) {
  }

  ngAfterViewInit() {
    this.initGoogleButton();
  }

  private initGoogleButton() {
    if (typeof google === 'undefined') {
      console.warn('Google script not loaded');
      return;
    }

    const containerWidth = this.googleBtn.nativeElement.clientWidth;

    try {
      google.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: (resp: any) => this.handleGoogleLogin(resp),
        auto_select: false,
        cancel_on_tap_outside: true
      });

      google.accounts.id.renderButton(this.googleBtn.nativeElement, {
        theme: 'outline',
        size: 'large',
        type: 'standard',
        width: containerWidth > 400 ? '400' : containerWidth.toString(),
        shape: 'rectangular',
        logo_alignment: 'center'
      });

      google.accounts.id.prompt();

    } catch (e) {
      console.error('Error initializing Google Sign-In:', e);
    }
  }

  private handleGoogleLogin(response: any) {
    const googleToken = response.credential;
    this.authService.googleLogin(googleToken).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.router.navigate(['/']);
        });
      },
      error: (err) => console.error('Error Google Login', err)
    });
  }
}
