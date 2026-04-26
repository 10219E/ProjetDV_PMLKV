import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NavService {
  private visibleSubject = new BehaviorSubject<boolean>(false);
  readonly visible$: Observable<boolean> = this.visibleSubject.asObservable();

  toggle(): void {
    this.visibleSubject.next(!this.visibleSubject.value);
  }

  open(): void {
    this.visibleSubject.next(true);
  }

  close(): void {
    this.visibleSubject.next(false);
  }
}

