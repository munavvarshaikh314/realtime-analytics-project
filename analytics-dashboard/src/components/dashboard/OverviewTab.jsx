import { Clock, Heart, MapPin, MessageSquare, Share2, ThumbsUp, Zap } from 'lucide-react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SensorTypeIcon from '@/components/dashboard/SensorTypeIcon'
import { buildSentimentTrendData, buildSensorTrendData, getSentimentColorClass, toDate } from '@/lib/dashboard-utils'

function OverviewTab({ realtimeIoT, realtimeSocial, iotData = [], socialData = [] }) {
  const sensorTrendData = buildSensorTrendData(iotData)
  const sentimentTrendData = buildSentimentTrendData(socialData)

  return (
    <>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Zap className="h-5 w-5 text-blue-600" />
              <span>Live IoT Data</span>
            </CardTitle>
            <CardDescription>Real-time sensor readings</CardDescription>
          </CardHeader>
          <CardContent>
            {realtimeIoT ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <SensorTypeIcon sensorType={realtimeIoT.sensorType} />
                    <span className="font-medium">{realtimeIoT.deviceId}</span>
                  </div>
                  <Badge variant={realtimeIoT.isAnomaly ? 'destructive' : 'default'}>
                    {realtimeIoT.isAnomaly ? 'Anomaly' : 'Normal'}
                  </Badge>
                </div>
                <div className="text-3xl font-bold text-blue-600">
                  {Number(realtimeIoT.sensorValue || 0).toFixed(2)} {realtimeIoT.unit || ''}
                </div>
                <div className="text-sm text-gray-500">
                  <div className="flex items-center space-x-1">
                    <MapPin className="h-3 w-3" />
                    <span>{realtimeIoT.location}</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <Clock className="h-3 w-3" />
                    <span>{toDate(realtimeIoT.timestamp)?.toLocaleString() || realtimeIoT.timestamp}</span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-8 text-center text-gray-500">Waiting for IoT data...</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Heart className="h-5 w-5 text-pink-600" />
              <span>Live Social Feed</span>
            </CardTitle>
            <CardDescription>Latest social media posts</CardDescription>
          </CardHeader>
          <CardContent>
            {realtimeSocial ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <Badge variant="outline">{realtimeSocial.platform}</Badge>
                  <div className={`text-sm font-medium ${getSentimentColorClass(realtimeSocial.sentimentScore)}`}>
                    {realtimeSocial.sentimentLabel}
                  </div>
                </div>
                <p className="line-clamp-3 text-sm">{realtimeSocial.content}</p>
                <div className="flex items-center space-x-4 text-sm text-gray-500">
                  <div className="flex items-center space-x-1">
                    <ThumbsUp className="h-3 w-3" />
                    <span>{realtimeSocial.likesCount || 0}</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <Share2 className="h-3 w-3" />
                    <span>{realtimeSocial.sharesCount || 0}</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <MessageSquare className="h-3 w-3" />
                    <span>{realtimeSocial.commentsCount || 0}</span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-8 text-center text-gray-500">Waiting for social media data...</div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Live Sensor Trends</CardTitle>
            <CardDescription>Temperature and humidity values from the latest IoT stream messages</CardDescription>
          </CardHeader>
          <CardContent>
            {sensorTrendData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={sensorTrendData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="temperature" stroke="#3b82f6" strokeWidth={2} connectNulls />
                  <Line type="monotone" dataKey="humidity" stroke="#10b981" strokeWidth={2} connectNulls />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-[300px] items-center justify-center text-center text-gray-500">
                Waiting for live IoT trend data...
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Live Sentiment Analysis</CardTitle>
            <CardDescription>Sentiment and engagement values from recent social stream events</CardDescription>
          </CardHeader>
          <CardContent>
            {sentimentTrendData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={sentimentTrendData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Area
                    type="monotone"
                    dataKey="sentiment"
                    stroke="#8b5cf6"
                    fill="#8b5cf6"
                    fillOpacity={0.3}
                    connectNulls
                  />
                  <Line type="monotone" dataKey="engagement" stroke="#f97316" strokeWidth={2} connectNulls />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-[300px] items-center justify-center text-center text-gray-500">
                Waiting for live social sentiment data...
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </>
  )
}

export default OverviewTab
