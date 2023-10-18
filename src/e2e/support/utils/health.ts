
import {fetch} from 'cross-fetch'
import { CustomWorld } from 'support/worlds/world';

export async function waitForHealthCheck(world: CustomWorld, timeout = 300000) {
    const startTime = Date.now();
    
    while (true) {
      try {
        console.error('Checking health');
        await world.openUrl('http://zaakafhandelcomponent-zac-dev.westeurope.cloudapp.azure.com')
        const frontend = await world.page.getByText('Sign in to your account').isVisible()
        
        
        // Check if the health check is successful.
        if (frontend) {
            console.error('Application is healthy');
          return true;
        } else {
            console.error('Not healthy yet');
        }
      } catch (error) {
        // Handle errors (e.g., service not available yet).
        console.error('Health check failed:', error.message);
      }
  
      // Check the timeout.
      if (Date.now() - startTime > timeout) {
        throw new Error('Health check timeout');
      }
  
      // Wait before the next attempt.
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }