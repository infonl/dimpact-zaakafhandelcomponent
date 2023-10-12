
import {fetch} from 'cross-fetch'
import { CustomWorld } from 'support/worlds/world';

export async function waitForHealthCheck(world: CustomWorld, timeout = 300000) {
    const startTime = Date.now();
    
    while (true) {
      try {
        console.error('Checking health');
        // Make a request to the health check endpoint.
        const zaaktypes = await world.request('http://localhost:8080/rest/health-check/zaaktypes')
        const communicatiekanaal = await world.request('http://localhost:8080/rest/health-check/bestaat-communicatiekanaal-eformulier')
        const ztcCache = await world.request('http://localhost:8080/rest/health-check/ztc-cache')
        const buildInformatie = await world.request('http://localhost:8080/rest/health-check/build-informatie')
        await world.openUrl('http://localhost:8080')
        const frontend = await world.page.getByText('Sign in to your account').isVisible()

        const responses = [
            zaaktypes,
            communicatiekanaal,
            ztcCache,
            buildInformatie,
        ]
        
        
        // Check if the health check is successful.
        if (responses.every(res => res.status === 200)  && frontend) {
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