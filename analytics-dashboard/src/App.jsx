import { lazy, Suspense, useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { Activity, AlertTriangle, Database, MessageSquare, TrendingUp, Wifi, Zap } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { createSampleIoTPayload, createSampleSocialPayload, formatLastUpdated } from '@/lib/dashboard-utils'
import './App.css'

const OverviewTab = lazy(() => import('@/components/dashboard/OverviewTab'))
const IotAnalyticsTab = lazy(() => import('@/components/dashboard/IotAnalyticsTab'))
const SocialMediaTab = lazy(() => import('@/components/dashboard/SocialMediaTab'))
const FinanceTab = lazy(() => import('@/components/dashboard/FinanceTab'))
const AlertsTab = lazy(() => import('@/components/dashboard/AlertsTab'))
const YouTubeTab = lazy(() => import('@/components/dashboard/YouTubeTab'))

const DEFAULT_API_BASE_URL = import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL).replace(/\/$/, '')
const DEFAULT_FINANCE_ADMIN_KEY = import.meta.env.VITE_FINANCE_ADMIN_KEY || ''
const TAB_LOADING_LABELS = {
  overview: 'Loading live overview...',
  iot: 'Loading IoT analytics...',
  social: 'Loading social analytics...',
  finance: 'Loading finance analytics...',
  alerts: 'Loading alerts...',
  youtube: 'Loading YouTube analytics...',
}

function getWebSocketUrl() {
  const configuredWebSocketUrl = import.meta.env.VITE_WS_URL
  if (configuredWebSocketUrl) {
    return configuredWebSocketUrl
  }

  const url = new URL(API_BASE_URL, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = '/ws-native'
  url.search = ''
  url.hash = ''

  return url.toString()
}

const WS_URL = getWebSocketUrl()

function App() {
  const [activeTab, setActiveTab] = useState('overview')
  const [isConnected, setIsConnected] = useState(false)
  const [iotData, setIotData] = useState([])
  const [socialData, setSocialData] = useState([])
  const [financeData, setFinanceData] = useState([])
  const [alerts, setAlerts] = useState([])
  const [metrics, setMetrics] = useState({
    iotMessageCount: 0,
    socialMediaMessageCount: 0,
    anomalyCount: 0,
    trendingPostCount: 0,
    topHashtags: [],
    timestamp: null,
  })
  const [realtimeIoT, setRealtimeIoT] = useState(null)
  const [realtimeSocial, setRealtimeSocial] = useState(null)
  const [realtimeFinance, setRealtimeFinance] = useState(null)
  const [youtubeData, setYoutubeData] = useState([])
  const [realtimeYouTube, setRealtimeYouTube] = useState(null)
  const [pendingRequest, setPendingRequest] = useState(null)
  const [requestStatus, setRequestStatus] = useState(null)
  const [financeActionStatus, setFinanceActionStatus] = useState(null)
  const [financeAdminKey, setFinanceAdminKey] = useState(DEFAULT_FINANCE_ADMIN_KEY)

  const topHashtags = metrics.topHashtags?.slice(0, 10) ?? []
  const latestTimestamp =
    metrics.timestamp ||
    realtimeFinance?.createdAt ||
    realtimeFinance?.eventTimestamp ||
    realtimeSocial?.timestamp ||
    realtimeSocial?.postTimestamp ||
    realtimeIoT?.timestamp
  const lastUpdatedLabel = formatLastUpdated(latestTimestamp)

  useEffect(() => {
    const stompClient = new Client({
      brokerURL: WS_URL,
      connectHeaders: {},
      debug: import.meta.env.DEV ? (message) => console.log(`STOMP: ${message}`) : () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    stompClient.onConnect = () => {
      setIsConnected(true)

      stompClient.subscribe('/topic/iot-updates', (message) => {
        const data = JSON.parse(message.body)
        setRealtimeIoT(data)
        setIotData((previous) => [...previous.slice(-19), data])
      })

      stompClient.subscribe('/topic/social-updates', (message) => {
        const data = JSON.parse(message.body)
        setRealtimeSocial(data)
        setSocialData((previous) => [...previous.slice(-19), data])
      })

      stompClient.subscribe('/topic/finance', (message) => {
        const data = JSON.parse(message.body)
        setRealtimeFinance(data)
        setFinanceData((previous) => [...previous.slice(-39), data])
      })

      stompClient.subscribe('/topic/youtube', (message) => {
        const data = JSON.parse(message.body)
        const videos = Array.isArray(data) ? data : [data]
        setRealtimeYouTube(videos)
        setYoutubeData((previous) => [...previous.slice(-19), ...videos])
      })

      stompClient.subscribe('/topic/alerts', (message) => {
        const alert = JSON.parse(message.body)
        setAlerts((previous) => [alert, ...previous.slice(0, 9)])
      })

      stompClient.subscribe('/topic/metrics', (message) => {
        setMetrics(JSON.parse(message.body))
      })
    }

    stompClient.onStompError = (frame) => {
      console.log(`Broker reported error: ${frame.headers.message || 'unknown error'}`)
      console.log(`Additional details: ${frame.body}`)
      setIsConnected(false)
    }

    stompClient.onDisconnect = () => {
      setIsConnected(false)
    }

    stompClient.onWebSocketClose = () => {
      setIsConnected(false)
    }

    stompClient.activate()

    return () => {
      setIsConnected(false)
      stompClient.deactivate()
    }
  }, [])

  useEffect(() => {
    async function loadInitialFinanceData() {
      try {
        const response = await fetch(`${API_BASE_URL}/api/finance/latest?limit=20`)
        if (!response.ok) {
          return
        }

        const data = await response.json()
        if (Array.isArray(data) && data.length > 0) {
          const ordered = [...data].reverse()
          setFinanceData(ordered)
          setRealtimeFinance(ordered[ordered.length - 1])
        }
      } catch (error) {
        console.log('Finance bootstrap skipped', error)
      }
    }

    loadInitialFinanceData()
  }, [])

  async function sendSampleRequest(requestType, endpoint, payload, successMessage) {
    setPendingRequest(requestType)
    setRequestStatus(null)

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      const responseBody = await response.json().catch(() => null)

      if (!response.ok) {
        throw new Error(responseBody?.message || `Request failed with status ${response.status}`)
      }

      const savedId = responseBody?.dataId ? ` ID ${responseBody.dataId}.` : ''
      setRequestStatus({
        type: 'success',
        message: `${successMessage}${savedId}`,
      })
    } catch (error) {
      setRequestStatus({
        type: 'error',
        message: error instanceof Error ? error.message : 'Unable to send sample data.',
      })
    } finally {
      setPendingRequest(null)
    }
  }

  const sendSampleIoTEvent = () => {
    sendSampleRequest('iot', '/api/data/iot/sensor', createSampleIoTPayload(), 'Sample IoT event submitted.')
  }

  const sendSampleSocialEvent = () => {
    sendSampleRequest(
      'social',
      '/api/data/social-media',
      createSampleSocialPayload(),
      'Sample social post submitted.'
    )
  }

  async function triggerFinanceRetrain() {
    setPendingRequest('finance-retrain')
    setFinanceActionStatus(null)

    try {
      const response = await fetch(`${API_BASE_URL}/api/finance/retrain`, {
        method: 'POST',
        headers: {
          'X-Analytics-Admin-Key': financeAdminKey,
        },
      })

      const body = await response.json().catch(() => null)
      if (!response.ok) {
        throw new Error(body?.message || `Retrain failed with status ${response.status}`)
      }

      setFinanceActionStatus({
        type: 'success',
        message: body?.message || 'Finance model retraining accepted.',
      })
    } catch (error) {
      setFinanceActionStatus({
        type: 'error',
        message: error instanceof Error ? error.message : 'Unable to trigger finance retraining.',
      })
    } finally {
      setPendingRequest(null)
    }
  }

  async function downloadFinanceReport(format) {
    setPendingRequest(`finance-${format}`)
    setFinanceActionStatus(null)

    try {
      const response = await fetch(`${API_BASE_URL}/api/finance/reports/export.${format}`, {
        headers: {
          'X-Analytics-Admin-Key': financeAdminKey,
        },
      })

      if (!response.ok) {
        const body = await response.json().catch(() => null)
        throw new Error(body?.message || `Download failed with status ${response.status}`)
      }

      const blob = await response.blob()
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = format === 'pdf' ? 'finance-report.pdf' : 'finance-report.csv'
      link.click()
      window.URL.revokeObjectURL(downloadUrl)

      setFinanceActionStatus({
        type: 'success',
        message: `Finance ${format.toUpperCase()} report downloaded.`,
      })
    } catch (error) {
      setFinanceActionStatus({
        type: 'error',
        message: error instanceof Error ? error.message : 'Unable to download finance report.',
      })
    } finally {
      setPendingRequest(null)
    }
  }

  function renderActiveTab() {
    switch (activeTab) {
      case 'iot':
        return (
          <TabsContent value="iot" className="space-y-6">
            <Suspense fallback={<TabLoadingState label={TAB_LOADING_LABELS.iot} />}>
              <IotAnalyticsTab iotData={iotData} />
            </Suspense>
          </TabsContent>
        )
      case 'social':
        return (
          <TabsContent value="social" className="space-y-6">
            <Suspense fallback={<TabLoadingState label={TAB_LOADING_LABELS.social} />}>
              <SocialMediaTab socialData={socialData} topHashtags={topHashtags} />
            </Suspense>
          </TabsContent>
        )
      case 'finance':
        return (
          <TabsContent value="finance" className="space-y-6">
            <Suspense fallback={<TabLoadingState label={TAB_LOADING_LABELS.finance} />}>
              <FinanceTab
                financeActionStatus={financeActionStatus}
                financeAdminKey={financeAdminKey}
                financeData={financeData}
                pendingRequest={pendingRequest}
                realtimeFinance={realtimeFinance}
                setFinanceAdminKey={setFinanceAdminKey}
                triggerFinanceRetrain={triggerFinanceRetrain}
                downloadFinanceReport={downloadFinanceReport}
              />
            </Suspense>
          </TabsContent>
        )
      case 'alerts':
        return (
          <TabsContent value="alerts" className="space-y-6">
            <Suspense fallback={<TabLoadingState label={TAB_LOADING_LABELS.alerts} />}>
              <AlertsTab alerts={alerts} />
            </Suspense>
          </TabsContent>
        )
      case 'youtube':
        return (
          <TabsContent value="youtube" className="space-y-6">
            <Suspense fallback={<TabLoadingState label={TAB_LOADING_LABELS.youtube} />}>
              <YouTubeTab youtubeData={youtubeData} realtimeYouTube={realtimeYouTube} />
            </Suspense>
          </TabsContent>
        )
      case 'overview':
      default:
        return (
          <TabsContent value="overview" className="space-y-6">
            <Suspense fallback={<TabLoadingState label={TAB_LOADING_LABELS.overview} />}>
              <OverviewTab
                iotData={iotData}
                realtimeIoT={realtimeIoT}
                realtimeSocial={realtimeSocial}
                socialData={socialData}
              />
            </Suspense>
          </TabsContent>
        )
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800">
      <header className="border-b bg-white shadow-sm dark:bg-slate-800">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between py-4">
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <Activity className="h-8 w-8 text-blue-600" />
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                  Real-time Analytics Dashboard
                </h1>
              </div>
              <Badge variant={isConnected ? 'default' : 'destructive'} className="flex items-center space-x-1">
                <Wifi className="h-3 w-3" />
                <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
              </Badge>
            </div>
            <div className="text-sm text-gray-500 dark:text-gray-400">Last updated: {lastUpdatedLabel}</div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-8 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
          <Card className="bg-gradient-to-r from-blue-500 to-blue-600 text-white">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">IoT Messages</CardTitle>
              <Database className="h-4 w-4" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{metrics.iotMessageCount}</div>
              <p className="text-xs opacity-80">Real-time sensor data</p>
            </CardContent>
          </Card>

          <Card className="bg-gradient-to-r from-green-500 to-green-600 text-white">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Social Posts</CardTitle>
              <MessageSquare className="h-4 w-4" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{metrics.socialMediaMessageCount}</div>
              <p className="text-xs opacity-80">Social media data</p>
            </CardContent>
          </Card>

          <Card className="bg-gradient-to-r from-red-500 to-red-600 text-white">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Anomalies</CardTitle>
              <AlertTriangle className="h-4 w-4" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{metrics.anomalyCount}</div>
              <p className="text-xs opacity-80">Detected anomalies</p>
            </CardContent>
          </Card>

          <Card className="bg-gradient-to-r from-purple-500 to-purple-600 text-white">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Trending</CardTitle>
              <TrendingUp className="h-4 w-4" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{metrics.trendingPostCount}</div>
              <p className="text-xs opacity-80">Trending posts</p>
            </CardContent>
          </Card>
        </div>

        <Card className="mb-8 border-blue-100 bg-white/80 backdrop-blur-sm dark:bg-slate-900/70">
          <CardHeader className="gap-3 lg:flex lg:flex-row lg:items-start lg:justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Zap className="h-5 w-5 text-blue-600" />
                <span>Pipeline Controls</span>
              </CardTitle>
              <CardDescription>
                Send live test events through the REST API, ML processing, Kafka streaming, and WebSocket updates.
              </CardDescription>
            </div>
            <div className="text-sm text-gray-500 dark:text-gray-400">REST API: {API_BASE_URL}</div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-col gap-3 sm:flex-row">
              <Button onClick={sendSampleIoTEvent} disabled={pendingRequest !== null}>
                {pendingRequest === 'iot' ? 'Sending IoT Event...' : 'Send Sample IoT Event'}
              </Button>
              <Button variant="outline" onClick={sendSampleSocialEvent} disabled={pendingRequest !== null}>
                {pendingRequest === 'social' ? 'Sending Social Post...' : 'Send Sample Social Post'}
              </Button>
            </div>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {isConnected
                ? 'The WebSocket stream is live. New events should appear in the dashboard moments after you send them.'
                : 'If the socket is disconnected, these buttons still help verify that the REST API is up and accepting data.'}
            </p>
            {requestStatus && (
              <div
                className={`rounded-md border px-3 py-2 text-sm ${
                  requestStatus.type === 'error'
                    ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-200'
                    : 'border-green-200 bg-green-50 text-green-700 dark:border-green-900/50 dark:bg-green-950/40 dark:text-green-200'
                }`}
              >
                {requestStatus.message}
              </div>
            )}
          </CardContent>
        </Card>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-6">
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="iot">IoT Analytics</TabsTrigger>
            <TabsTrigger value="social">Social Media</TabsTrigger>
            <TabsTrigger value="finance">Finance</TabsTrigger>
            <TabsTrigger value="youtube">YouTube</TabsTrigger>
            <TabsTrigger value="alerts">Alerts</TabsTrigger>
          </TabsList>

          {renderActiveTab()}
        </Tabs>
      </main>
    </div>
  )
}

function TabLoadingState({ label }) {
  return (
    <Card>
      <CardContent className="flex min-h-[280px] items-center justify-center text-sm text-gray-500">
        {label}
      </CardContent>
    </Card>
  )
}

export default App
