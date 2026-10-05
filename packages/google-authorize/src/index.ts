import { registerPlugin } from '@capacitor/core';

import type { GoogleAuthorizePlugin } from './definitions';

// No web implementation: in a browser each method rejects with UNIMPLEMENTED
const GoogleAuthorize = registerPlugin<GoogleAuthorizePlugin>('GoogleAuthorize');

export * from './definitions';
export { GoogleAuthorize };
