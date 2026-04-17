import { Play, ThumbsUp, MessageSquare, TrendingUp, Eye, User } from 'lucide-react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'

function YouTubeTab({ realtimeYouTube, youtubeData = [] }) {
  // Build trend data for charts
  const buildViewTrendData = (data) => {
    return data
      .filter(item => item.viewCount)
      .slice(-20)
      .map((item, index) => ({
        time: new Date(item.fetchedAt || Date.now() - (19 - index) * 60000).toLocaleTimeString(),
        views: item.viewCount,
        likes: item.likeCount || 0,
        comments: item.commentCount || 0,
      }))
  }

  const viewTrendData = buildViewTrendData(youtubeData)

  // Calculate engagement metrics
  const calculateEngagementMetrics = (data) => {
    if (!data || data.length === 0) return null

    const totalViews = data.reduce((sum, item) => sum + (item.viewCount || 0), 0)
    const totalLikes = data.reduce((sum, item) => sum + (item.likeCount || 0), 0)
    const totalComments = data.reduce((sum, item) => sum + (item.commentCount || 0), 0)

    const avgEngagementRate = data.length > 0 ?
      data.reduce((sum, item) => {
        const views = item.viewCount || 0
        const engagement = (item.likeCount || 0) + (item.commentCount || 0)
        return sum + (views > 0 ? engagement / views : 0)
      }, 0) / data.length : 0

    return {
      totalViews,
      totalLikes,
      totalComments,
      avgEngagementRate: avgEngagementRate * 100, // Convert to percentage
      videoCount: data.length
    }
  }

  const metrics = calculateEngagementMetrics(youtubeData)

  return (
    <>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Play className="h-5 w-5 text-red-600" />
              <span>Live YouTube Trending</span>
            </CardTitle>
            <CardDescription>Real-time trending video data</CardDescription>
          </CardHeader>
          <CardContent>
            {realtimeYouTube && realtimeYouTube.length > 0 ? (
              <div className="space-y-4">
                {realtimeYouTube.slice(0, 3).map((video, index) => (
                  <div key={video.videoId || index} className="border rounded-lg p-4 space-y-2">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <h4 className="font-medium text-sm line-clamp-2">{video.title}</h4>
                        <p className="text-xs text-gray-500 flex items-center space-x-1">
                          <User className="h-3 w-3" />
                          <span>{video.channelTitle}</span>
                        </p>
                      </div>
                      {video.analysis && video.analysis.isHighlyTrending && (
                        <Badge variant="default" className="ml-2">
                          <TrendingUp className="h-3 w-3 mr-1" />
                          Trending
                        </Badge>
                      )}
                    </div>

                    <div className="flex items-center space-x-4 text-xs text-gray-600">
                      <div className="flex items-center space-x-1">
                        <Eye className="h-3 w-3" />
                        <span>{(video.viewCount || 0).toLocaleString()}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <ThumbsUp className="h-3 w-3" />
                        <span>{(video.likeCount || 0).toLocaleString()}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <MessageSquare className="h-3 w-3" />
                        <span>{(video.commentCount || 0).toLocaleString()}</span>
                      </div>
                    </div>

                    {video.analysis && (
                      <div className="space-y-2">
                        <div className="flex items-center space-x-2">
                          <span className="text-xs text-gray-500">Virality:</span>
                          <Progress value={video.analysis.viralityScore * 100} className="flex-1 h-2" />
                          <span className="text-xs font-medium">
                            {(video.analysis.viralityScore * 100).toFixed(1)}%
                          </span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <span className="text-xs text-gray-500">Sentiment:</span>
                          <Badge
                            variant={
                              video.analysis.titleSentiment > 0.2 ? "default" :
                              video.analysis.titleSentiment < -0.2 ? "destructive" : "secondary"
                            }
                            className="text-xs"
                          >
                            {video.analysis.titleSentiment > 0.2 ? "Positive" :
                             video.analysis.titleSentiment < -0.2 ? "Negative" : "Neutral"}
                          </Badge>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex items-center justify-center h-32 text-gray-500">
                <div className="text-center">
                  <Play className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p>No trending videos data available yet</p>
                <p className="text-sm">
                  Waiting for backend YouTube feed. Make sure the backend is running with
                  <span className="font-medium"> analytics.connectors.youtube.enabled=true</span>
                  and a valid <span className="font-medium">YOUTUBE_API_KEY</span>.
                </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <TrendingUp className="h-5 w-5 text-blue-600" />
              <span>Engagement Metrics</span>
            </CardTitle>
            <CardDescription>Overall YouTube analytics</CardDescription>
          </CardHeader>
          <CardContent>
            {metrics ? (
              <div className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                  <div className="text-center">
                    <div className="text-2xl font-bold text-blue-600">
                      {metrics.totalViews.toLocaleString()}
                    </div>
                    <div className="text-sm text-gray-500">Total Views</div>
                  </div>
                  <div className="text-center">
                    <div className="text-2xl font-bold text-green-600">
                      {metrics.totalLikes.toLocaleString()}
                    </div>
                    <div className="text-sm text-gray-500">Total Likes</div>
                  </div>
                  <div className="text-center">
                    <div className="text-2xl font-bold text-purple-600">
                      {metrics.totalComments.toLocaleString()}
                    </div>
                    <div className="text-sm text-gray-500">Total Comments</div>
                  </div>
                  <div className="text-center">
                    <div className="text-2xl font-bold text-orange-600">
                      {metrics.videoCount}
                    </div>
                    <div className="text-sm text-gray-500">Videos Tracked</div>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium">Avg Engagement Rate</span>
                    <span className="text-sm text-gray-500">
                      {metrics.avgEngagementRate.toFixed(2)}%
                    </span>
                  </div>
                  <Progress value={Math.min(metrics.avgEngagementRate, 100)} className="h-2" />
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-center h-32 text-gray-500">
                <p>No metrics available</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>View Trends Over Time</CardTitle>
          <CardDescription>YouTube video performance metrics</CardDescription>
        </CardHeader>
        <CardContent>
          {viewTrendData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={viewTrendData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis
                  dataKey="time"
                  tick={{ fontSize: 12 }}
                  interval="preserveStartEnd"
                />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip
                  formatter={(value, name) => [
                    typeof value === 'number' ? value.toLocaleString() : value,
                    name === 'views' ? 'Views' : name === 'likes' ? 'Likes' : 'Comments'
                  ]}
                />
                <Line
                  type="monotone"
                  dataKey="views"
                  stroke="#dc2626"
                  strokeWidth={2}
                  dot={{ r: 3 }}
                  name="views"
                />
                <Line
                  type="monotone"
                  dataKey="likes"
                  stroke="#16a34a"
                  strokeWidth={2}
                  dot={{ r: 3 }}
                  name="likes"
                />
                <Line
                  type="monotone"
                  dataKey="comments"
                  stroke="#7c3aed"
                  strokeWidth={2}
                  dot={{ r: 3 }}
                  name="comments"
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-64 text-gray-500">
              <p>No trend data available</p>
            </div>
          )}
        </CardContent>
      </Card>
    </>
  )
}

export default YouTubeTab