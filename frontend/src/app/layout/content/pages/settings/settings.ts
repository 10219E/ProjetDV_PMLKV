import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, RouterModule, NavMenu, HomeAccountHeader],
  templateUrl: './settings.html',
  styleUrls: ['./settings.css']
})
export class SettingsComponent {}
