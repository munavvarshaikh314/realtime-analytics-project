import { Hash, MessageSquare, Share2, ThumbsUp } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { getSentimentColorClass } from '@/lib/dashboard-utils'

function SocialMediaTab({ socialData = [], topHashtags = [] }) {
  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle>Social Media Feed</CardTitle>
          <CardDescription>Recent posts and analysis</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="max-h-96 space-y-4 overflow-y-auto">
            {socialData.length > 0 ? (
              socialData.map((data, index) => (
                <div
                  key={data.postId || `${data.platform || 'post'}-${index}`}
                  className="rounded-lg bg-gray-50 p-4 dark:bg-gray-800"
                >
                  <div className="mb-2 flex items-center justify-between">
                    <Badge variant="outline">{data.platform}</Badge>
                    <div className={`text-sm font-medium ${getSentimentColorClass(data.sentimentScore)}`}>
                      {data.sentimentLabel}
                    </div>
                  </div>
                  <p className="mb-3 line-clamp-2 text-sm">{data.content}</p>
                  <div className="flex items-center space-x-4 text-xs text-gray-500">
                    <span className="flex items-center gap-1">
                      <ThumbsUp className="h-3 w-3" />
                      {data.likesCount || 0}
                    </span>
                    <span className="flex items-center gap-1">
                      <Share2 className="h-3 w-3" />
                      {data.sharesCount || 0}
                    </span>
                    <span className="flex items-center gap-1">
                      <MessageSquare className="h-3 w-3" />
                      {data.commentsCount || 0}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-8 text-center text-gray-500">No social media data received yet</div>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Trending Hashtags</CardTitle>
          <CardDescription>Most popular hashtags from the live stream</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {topHashtags.length > 0 ? (
              topHashtags.map((hashtag, index) => (
                <div key={`${hashtag.hashtag || 'hashtag'}-${index}`} className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Hash className="h-4 w-4 text-blue-600" />
                    <span className="font-medium">{hashtag.hashtag}</span>
                  </div>
                  <Badge variant="secondary">{hashtag.count}</Badge>
                </div>
              ))
            ) : (
              <div className="py-8 text-center text-gray-500">No hashtag data available</div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

export default SocialMediaTab
