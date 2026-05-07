/// <reference types="cypress" />
/// <reference path="../../../../../../cypress/support/component.ts" />
import { HomeComponent } from './home';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('HomeComponent', () => {
  beforeEach(() => {
    // Set a standard viewport to ensure elements are visible
    cy.viewport(1280, 720);
  });

  it('should open and close the login modal', () => {
    cy.mount(HomeComponent, {
      imports: [HttpClientModule, BrowserAnimationsModule, RouterTestingModule],
    });

    // Verify modal is not visible initially
    cy.get('h2').contains('Connexion').should('not.exist');

    // Click "Se connecter" button in header
    cy.get('button').contains('Se connecter').click();

    // Verify modal is visible
    cy.get('h2').contains('Connexion').should('be.visible');

    // Click close button
    // The close button is the first button inside the modal overlay
    cy.get('button').find('svg').first().click();

    // Verify modal is gone
    cy.get('h2').contains('Connexion').should('not.exist');
  });

  it('should show validation error for invalid email (@if check)', () => {
    cy.mount(HomeComponent, {
      imports: [HttpClientModule, BrowserAnimationsModule, RouterTestingModule],
    });

    cy.get('button').contains('Se connecter').click();

    // Type invalid email
    cy.get('input[id="loginEmail"]').type('not-an-email').blur();

    // Check @if logic renders the error
    cy.get('.text-red-500').should('be.visible').and('contain', "L'identifiant doit être sous forme d' e-mail.");
  });

  it('should disable login button when form is invalid', () => {
    cy.mount(HomeComponent, {
      imports: [HttpClientModule, BrowserAnimationsModule, RouterTestingModule],
    });

    cy.get('button').contains('Se connecter').click();

    // Button should be disabled initially (or logic should prevent submission)
    // Looking at the submit button logic in template might be needed, but usually it's [disabled]="loginForm.invalid"
    cy.get('button[type="submit"]').should('be.disabled');
  });
});
