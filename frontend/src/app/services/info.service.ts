import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class InfoService {

  private base = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getCounts(): Observable<{ sites: number; fields: number }> {
    return this.http.get<{ sites: number; fields: number }>(`${this.base}/fscount`);
  }

  getSites(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/sitelist`);
  }
}
