import { ApplicationInsights } from '@microsoft/applicationinsights-web'

console.log(process.env)

function loadAppInsights() {
    const appInsights = new ApplicationInsights({ config: {
      instrumentationKey: process.env.REACT_APP_APPINSIGHTS_INSTRUMENTATIONKEY,
      disableFetchTracking: false,
      autoTrackPageVisitTime: true,
    } });
    appInsights.loadAppInsights();
    return appInsights
}

export { loadAppInsights }