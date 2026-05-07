// cypress.config.ts
import { defineConfig } from "cypress";

export default defineConfig({
  allowCypressEnv: false,
  component: {
    devServer: {
      framework: "angular",
      bundler: "webpack",
    },
    specPattern: "**/*.cy.ts",
    reporter: 'junit',
    reporterOptions: {
      mochaFile: 'cypress/results/results-[hash].xml',
      testsuitesTitle: true,
    }
  },
});
