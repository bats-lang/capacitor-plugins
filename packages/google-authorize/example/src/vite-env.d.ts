/// <reference types="vite/client" />

interface ImportMetaEnv {
  // The commit the demo was built from, set by .github/workflows/demo-apk.yml
  readonly VITE_DEMO_COMMIT?: string;
}
