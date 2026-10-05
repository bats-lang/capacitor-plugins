import { registerPlugin } from '@capacitor/core';

import type { GoogleAuthorizePlugin } from './definitions';

const GoogleAuthorize = registerPlugin<GoogleAuthorizePlugin>('GoogleAuthorize');

export * from './definitions';
export { GoogleAuthorize };
