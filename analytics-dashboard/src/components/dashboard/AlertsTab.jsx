import { AlertTriangle } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { getAlertSeverityVariant, toDate } from '@/lib/dashboard-utils'

function AlertsTab({ alerts = [] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>System Alerts</CardTitle>
        <CardDescription>Recent alerts and notifications</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {alerts.length > 0 ? (
            alerts.map((alert, index) => (
              <Alert
                key={`${alert.alertType || 'alert'}-${alert.timestamp || index}`}
                className="border-l-4 border-l-red-500"
              >
                <AlertTriangle className="h-4 w-4" />
                <AlertTitle className="flex items-center justify-between">
                  <span>{alert.alertType}</span>
                  <Badge variant={getAlertSeverityVariant(alert.severity)}>{alert.severity}</Badge>
                </AlertTitle>
                <AlertDescription>
                  <div className="mt-2 text-sm">
                    <div>Time: {toDate(alert.timestamp)?.toLocaleString() || alert.timestamp}</div>
                    {alert.data && (
                      <div className="mt-1">
                        Source: {alert.data.deviceId || alert.data.platform || alert.data.symbol || 'Unknown'}
                      </div>
                    )}
                  </div>
                </AlertDescription>
              </Alert>
            ))
          ) : (
            <div className="py-8 text-center text-gray-500">No alerts at this time</div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

export default AlertsTab
