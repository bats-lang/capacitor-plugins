import { registerPlugin } from '@capacitor/core';

import type { HelloPlugin } from './definitions';

const Hello = registerPlugin<HelloPlugin>('Hello');

export * from './definitions';
export { Hello };
