import { Pie, PieChart, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SensorTypeIcon from '@/components/dashboard/SensorTypeIcon'
import { buildSensorDistribution } from '@/lib/dashboard-utils'

function IotAnalyticsTab({ iotData = [] }) {
  const sensorDistributionData = buildSensorDistribution(iotData)

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <CardHeader>
          <CardTitle>IoT Data Stream</CardTitle>
          <CardDescription>Recent sensor readings</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="max-h-96 space-y-4 overflow-y-auto">
            {iotData.length > 0 ? (
              iotData.map((data, index) => (
                <div
                  key={`${data.deviceId || 'device'}-${data.timestamp || index}`}
                  className="flex items-center justify-between rounded-lg bg-gray-50 p-3 dark:bg-gray-800"
                >
                  <div className="flex items-center space-x-3">
                    <SensorTypeIcon sensorType={data.sensorType} />
                    <div>
                      <div className="font-medium">{data.deviceId}</div>
                      <div className="text-sm text-gray-500">{data.sensorType}</div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-bold">
                      {Number.isFinite(Number(data.sensorValue)) ? Number(data.sensorValue).toFixed(2) : '-'}
                    </div>
                    <Badge variant={data.isAnomaly ? 'destructive' : 'default'} className="text-xs">
                      {data.isAnomaly ? 'Anomaly' : 'Normal'}
                    </Badge>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-8 text-center text-gray-500">No IoT data received yet</div>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Sensor Distribution</CardTitle>
          <CardDescription>Live distribution of received sensor types</CardDescription>
        </CardHeader>
        <CardContent>
          {sensorDistributionData.length > 0 ? (
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie data={sensorDistributionData} cx="50%" cy="50%" outerRadius={80} dataKey="value" label>
                  {sensorDistributionData.map((entry) => (
                    <Cell key={entry.name} fill={entry.fill} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-[250px] items-center justify-center text-center text-gray-500">
              Waiting for sensor distribution data...
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default IotAnalyticsTab
