# awesome-angular

A clean Angular project starter with Angular CLI, ESLint + Prettier, tests (Karma/Jasmine), Husky pre-commit, and CI.

## Prereqs
- Node 18+ (LTS recommended)
- PNPM, npm, or Yarn (examples use `npm`)

## Quickstart
```bash
# install deps
npm install

# start dev server
npm run start

# run tests
npm test

# lint & fix
npm run lint
npm run format

# production build
npm run build
```

## What's inside
- Angular CLI workspace (`angular.json`, tsconfigs)
- Standalone `AppComponent` (no NgModule)
- ESLint via `@angular-eslint/*` and Prettier
- Husky + lint-staged pre-commit
- GitHub Actions workflow (`.github/workflows/ci.yml`)

## Rename the app
- Search & replace `awesome-angular` in `angular.json` and `package.json` if you want a different app name.
