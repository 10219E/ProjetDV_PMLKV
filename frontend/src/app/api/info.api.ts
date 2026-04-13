import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InfoControllerDto } from '../models/info.model';

@Injectable({
  providedIn: 'root'
})
export class InfoApi {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  //GET COUNT FOR SITES AND FIELDS
  public getCounts(): Observable<InfoControllerDto> {
    return this.http.get<InfoControllerDto>(`${this.apiUrl}/fscount`);
  }

  //GET SITES (ADDRESS + NAME + ID)
  public getSites(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/sitelist`);
  }
}

