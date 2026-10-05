// The bundles an app that loads plugins without a bundler takes: an IIFE
// (window.capacitorGoogleAuthorize) and CommonJS, from tsc's ES modules.
export default {
  input: 'dist/esm/index.js',
  output: [
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorGoogleAuthorize',
      globals: { '@capacitor/core': 'capacitorExports' },
      sourcemap: true,
      inlineDynamicImports: true,
    },
    {
      file: 'dist/plugin.cjs.js',
      format: 'cjs',
      sourcemap: true,
      inlineDynamicImports: true,
    },
  ],
  external: ['@capacitor/core'],
};
