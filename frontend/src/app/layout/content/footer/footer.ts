import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InfoService } from '../../../services/info.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './footer.html',
  styleUrl: './footer.css',
})
export class Footer implements OnInit {
  sites: Array<{ siteId: number; name: string; address: string }> = [];

  constructor(private infoService: InfoService) {}

  ngOnInit(): void {
    this.infoService.getSites().subscribe({
      next: (data: any) => {
        if (data && Array.isArray(data.siteInfoList)) {
          this.sites = data.siteInfoList;
        } else if (Array.isArray(data)) {
          // fallback if service returns array directly
          this.sites = data;
        } else {
          this.sites = [];
        }
      },
      error: (err) => {
        console.error('Failed to load sites for footer', err);
        this.sites = [];
      }
    });
  }

}
