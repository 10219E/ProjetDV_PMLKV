import { Injectable } from '@angular/core';
import { StatisticsService as ApiStatisticsService, FinancialRecordDto } from '../api';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {

  constructor(private apiStatistics: ApiStatisticsService, private authService: AuthService) { }

  getFinancialReport(): Observable<Array<FinancialRecordDto>> {
    this.setAuthHeader();
    return this.apiStatistics.getFinancialReport();
  }

  getFinancialReportBySite(siteId: number): Observable<Array<FinancialRecordDto>> {
    this.setAuthHeader();
    return this.apiStatistics.getFinancialReportBySite(siteId);
  }

  private setAuthHeader(): void {
    try {
      const token = this.authService.getToken();
      if (token && this.apiStatistics && this.apiStatistics.defaultHeaders && this.apiStatistics.defaultHeaders.set) {
        this.apiStatistics.defaultHeaders = this.apiStatistics.defaultHeaders.set('Authorization', `Bearer ${token}`);
      }
    } catch (e) {
      // ignore
    }
  }
}
