import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { InfoApi } from '../api/info.api';
import { InfoControllerDto } from '../models/info.model';

@Injectable({ providedIn: 'root' })
export class InfoService {

  constructor(private infoApi: InfoApi) {}

  getCounts(): Observable<InfoControllerDto> {
    return this.infoApi.getCounts();
  }

  getSites(): Observable<any[]> {
    return this.infoApi.getSites();
  }
}
