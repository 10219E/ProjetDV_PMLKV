import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { InfoControllerService } from '../api/api/infoController.service';
import { InfoControllerDto } from '../api/model/infoControllerDto';
import { map } from 'rxjs/operators';
import { SiteInfo } from '../api/model/siteInfo';

@Injectable({ providedIn: 'root' })
export class InfoService {

  constructor(private infoControllerService: InfoControllerService) {}

  getCounts(): Observable<InfoControllerDto> {
    return this.infoControllerService.getSitesAndFieldsCount();
  }

  getSites(): Observable<SiteInfo[]> {
    return this.infoControllerService.getSites().pipe(
      map(dto => dto.siteInfoList || [])
    );
  }

  getMatriculeByEmail(email: string): Observable<string | undefined> {
    return this.infoControllerService.getUserByEmail(email).pipe(
      map(profile => profile?.matricule)
    );
  }

}
