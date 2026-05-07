/// <reference types="cypress" />
/// <reference path="../../../../../cypress/support/component.ts" />
import { UserFormComponent } from './user-form';
import { HttpClientModule } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../services/auth.service';

describe('UserFormComponent', () => {
  beforeEach(() => {
    cy.viewport(1280, 800);
  });

  const mountUserForm = (options: any = {}) => {
    const mockSites = [
      { siteId: 1, name: 'Site A', address: '123 Street' },
      { siteId: 2, name: 'Site B', address: '456 Avenue' }
    ];

    cy.mount(UserFormComponent, {
      imports: [HttpClientModule, RouterTestingModule, BrowserAnimationsModule],
      providers: [
        {
          provide: AuthService,
          useValue: {
            signup: options.signupMock ?? (() => of({ success: true }))
          }
        }
      ],
      componentProperties: {
        sites: options.sites ?? mockSites,
        inviteMode: options.inviteMode ?? false,
        prefillEmail: options.prefillEmail ?? null,
        prefillSiteId: options.prefillSiteId ?? null,
        prefillSiteName: options.prefillSiteName ?? null
      }
    }).then((wrapper) => {
      wrapper.fixture.detectChanges();
    });

    cy.wait(100);
  };

  it('should display validation errors for empty required fields', () => {
    mountUserForm();

    // Touch fields to trigger validation
    cy.get('#fname').focus().blur();
    cy.get('#lname').focus().blur();
    cy.get('#email').focus().blur();

    cy.contains(/prénom est requis/i).should('be.visible');
    cy.contains(/nom est requis/i).should('be.visible');
    cy.contains(/email invalide/i).should('be.visible');
  });

  it('should validate password matching', () => {
    mountUserForm();

    cy.get('#password').type('Pass123!');
    cy.get('#confirmPassword').type('Different123!').blur();

    cy.contains(/mots de passe ne correspondent pas/i).should('be.visible');
  });

  it('should restrict age to minimum 16 years', () => {
    mountUserForm();

    const youngDate = new Date();
    youngDate.setFullYear(youngDate.getFullYear() - 10);

    const year = youngDate.getFullYear();
    const month = String(youngDate.getMonth() + 1).padStart(2, '0');
    const day = String(youngDate.getDate()).padStart(2, '0');
    const dateString = `${year}-${month}-${day}`;

    cy.get('#bdate').type(dateString).blur();

    // Using a broader regex to match the French text "Vous devez avoir au moins 16 ans"
    // The previous error might have been due to a typo in the test vs the template (e.g., 'Vouz' vs 'Vous')
    cy.contains(/avoir au moins 16 ans/i).should('be.visible');
  });

  it('should successfuly submit the form, hide it and show success message', () => {
    mountUserForm();

    // Fill valid data
    cy.get('#fname').type('Jean');
    cy.get('#lname').type('Dupont');
    cy.get('#email').type('jean.dupont@example.com');
    cy.get('#password').type('Complex123!');
    cy.get('#confirmPassword').type('Complex123!');
    cy.get('#bdate').type('2000-01-01');
    cy.get('select#siteId').select(1); // Select Site A
    cy.contains('button', /Débutant/i).click();

    cy.get('button[type="submit"]').should('not.be.disabled').click();

    // Form should disappear and success message appear
    cy.contains(/Inscription réussie/i).should('be.visible');
    cy.contains(/Se connecter/i).should('be.visible');
  });

  it('should handle signup error from API', () => {
    const signupErrorMock = () => throwError(() => new Error('API Error'));
    mountUserForm({ signupMock: signupErrorMock });

    // Fill valid data
    cy.get('#fname').type('Jean');
    cy.get('#lname').type('Dupont');
    cy.get('#email').type('jean.dupont@example.com');
    cy.get('#password').type('Complex123!');
    cy.get('#confirmPassword').type('Complex123!');
    cy.get('#bdate').type('2000-01-01');
    cy.get('select#siteId').select(1);
    cy.contains('button', /Débutant/i).click();

    cy.get('button[type="submit"]').click();

    // Error message from @if (signupError)
    cy.contains(/erreur est survenue lors de l'inscription/i).should('be.visible');
  });

  it('should lock email and site when in inviteMode and prefilled', () => {
    mountUserForm({
       inviteMode: true,
       prefillEmail: 'invite@test.com',
       prefillSiteId: 1,
       prefillSiteName: 'Locked Site'
    });

    cy.get('#email').should('be.disabled').and('have.value', 'invite@test.com');
    cy.get('input[readonly]').should('have.value', 'Locked Site');
  });
});

