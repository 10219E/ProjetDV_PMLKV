import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthLoginRequest, AuthLoginResponse, UserRegistrationRequest } from '../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthApi {
  private apiUrl = 'http://localhost:8080/auth';

  constructor(private http: HttpClient) { }

  //AUTHENTICATION
  public login(request: AuthLoginRequest): Observable<AuthLoginResponse> {
    return this.http.post<AuthLoginResponse>(`${this.apiUrl}/login`, request);
  }

  //REGISTRATION
  public register(userData: UserRegistrationRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, userData);
  }
}

