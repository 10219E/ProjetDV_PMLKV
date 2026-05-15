import { Injectable } from '@angular/core';
import { SiteControllerService } from '../api';
import { SessionService } from './session.service';
import { Observable } from 'rxjs';
import { Site } from '../api';

@Injectable({
  providedIn: 'root'
})
export class SiteService {

  constructor(
    private siteController: SiteControllerService,
    private sessionService: SessionService
  ) { }

  getAllSites(active?: boolean): Observable<Site[]> {
    this.sessionService.setAuthHeader(this.siteController);
    return this.siteController.getAllSites(active);
  }
}
