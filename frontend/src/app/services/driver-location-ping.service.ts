import {DestroyRef, effect, inject, Injectable, OnDestroy} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AuthService} from './auth.service';
import {NotificationService} from './notification.service';
import {DriverLocation} from '../models/driver-location.model';

@Injectable({
  providedIn: 'root'
})
export class DriverLocationPingService implements OnDestroy {

  private readonly PING_INTERVAL_MS = 3 * 60 * 1000; // 3 minutes
  private readonly apiUrl = '/api/v1/trucks/driver-locations';

  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);

  getAllDriverLocations(): Observable<DriverLocation[]> {
    return this.http.get<DriverLocation[]>(this.apiUrl);
  }

  private intervalHandle: ReturnType<typeof setInterval> | null = null;
  private lastPosition: { lat: number; lng: number } | null = null;

  constructor() {
    effect(() => {
      const shouldPing = this.authService.isLogged()
        && this.authService.isDriver()
        && this.notificationService.isSocketOpen();

      if (shouldPing && this.intervalHandle === null) {
        // Fire one ping immediately on WS open, then schedule the periodic ones
        this.sendPing();
        this.startPinging();
      } else if (!shouldPing && this.intervalHandle !== null) {
        this.stopPinging();
      }
    });

    // Flush a final ping synchronously while the WS is still OPEN (logout or tab close)
    this.notificationService.beforeDisconnect$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.flushLastPing());
  }

  ngOnDestroy(): void {
    this.stopPinging();
  }

  private flushLastPing(): void {
    // No isDriver() check here: by the time this fires on explicit logout, the auth signal
    // has already been cleared. lastPosition can only be non-null if the driver successfully
    // pinged at least once this session, which is guard enough.
    if (!this.lastPosition) {
      return;
    }
    this.notificationService.send({
      topic: 'GPS_PING',
      lat: this.lastPosition.lat,
      lng: this.lastPosition.lng
    });
    this.lastPosition = null;
  }

  private startPinging(): void {
    this.intervalHandle = setInterval(() => this.sendPing(), this.PING_INTERVAL_MS);
  }

  private stopPinging(): void {
    if (this.intervalHandle !== null) {
      clearInterval(this.intervalHandle);
      this.intervalHandle = null;
    }
  }

  private sendPing(): void {
    if (!navigator.geolocation) {
      console.warn('Geolocation API not available in this browser; skipping GPS ping.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.lastPosition = {lat: position.coords.latitude, lng: position.coords.longitude};
        this.notificationService.send({
          topic: 'GPS_PING',
          lat: position.coords.latitude,
          lng: position.coords.longitude
        });
      },
      (error) => {
        console.warn('Could not obtain driver geolocation:', error.message);
      },
      {enableHighAccuracy: true, timeout: 10000, maximumAge: 60000}
    );
  }
}
