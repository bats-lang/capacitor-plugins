import javascript from '@eslint/js';
import typescript from 'typescript-eslint';

export default typescript.config(
  {
    ignores: ['**/node_modules/', '**/dist/', '**/android/', '**/build/'],
  },
  javascript.configs.recommended,
  ...typescript.configs.recommended,
);
