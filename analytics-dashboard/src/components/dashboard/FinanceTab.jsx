import { BarChart3, Bitcoin, Download, RefreshCcw } from 'lucide-react'
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  formatChartTime,
  formatCompactNumber,
  formatCurrency,
  getFinanceSignalColorClass,
  getFinanceSignalVariant,
  toNumber,
} from '@/lib/dashboard-utils'

function FinanceTab({
  financeActionStatus,
  financeAdminKey,
  financeData = [],
  pendingRequest,
  realtimeFinance,
  setFinanceAdminKey,
  triggerFinanceRetrain,
  downloadFinanceReport,
}) {
  const selectedFinanceSymbol = realtimeFinance?.symbol || financeData[financeData.length - 1]?.symbol
  const financeTrendData = financeData
    .filter((event) => !selectedFinanceSymbol || event.symbol === selectedFinanceSymbol)
    .slice(-20)
    .map((event, index) => ({
      time: formatChartTime(event.createdAt || event.eventTimestamp, `Tick ${index + 1}`),
      currentPrice: toNumber(event.currentPrice),
      predictedPrice: toNumber(event.predictedPrice),
      anomalyScore: toNumber(event.anomalyScore),
    }))
  const liveSources = Array.from(
    new Set(financeData.map((item) => `${item.provider || 'Unknown'}|${item.marketSegment || 'FINANCE'}`))
  )

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bitcoin className="h-5 w-5 text-amber-500" />
            <span>Finance Controls</span>
          </CardTitle>
          <CardDescription>
            Manage retraining and export reports for the CoinGecko and Polygon finance pipeline.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto_auto_auto]">
            <Input
              type="password"
              value={financeAdminKey}
              onChange={(event) => setFinanceAdminKey(event.target.value)}
              placeholder="Finance admin key"
            />
            <Button onClick={triggerFinanceRetrain} disabled={pendingRequest !== null}>
              <RefreshCcw className="h-4 w-4" />
              <span>{pendingRequest === 'finance-retrain' ? 'Retraining...' : 'Retrain Model'}</span>
            </Button>
            <Button variant="outline" onClick={() => downloadFinanceReport('pdf')} disabled={pendingRequest !== null}>
              <Download className="h-4 w-4" />
              <span>{pendingRequest === 'finance-pdf' ? 'Preparing PDF...' : 'Export PDF'}</span>
            </Button>
            <Button variant="outline" onClick={() => downloadFinanceReport('csv')} disabled={pendingRequest !== null}>
              <Download className="h-4 w-4" />
              <span>{pendingRequest === 'finance-csv' ? 'Preparing CSV...' : 'Export CSV'}</span>
            </Button>
          </div>

          <div className="flex flex-wrap items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
            <span>Live sources:</span>
            {liveSources.length > 0 ? (
              liveSources.map((source) => {
                const [provider, marketSegment] = source.split('|')
                return (
                  <Badge key={source} variant="outline">
                    {provider} {marketSegment}
                  </Badge>
                )
              })
            ) : (
              <Badge variant="secondary">Awaiting live market feeds</Badge>
            )}
          </div>

          <p className="text-sm text-gray-500 dark:text-gray-400">
            CoinGecko crypto runs by default. Polygon licensed equities join the same Kafka topic after you set
            <span className="font-mono"> POLYGON_API_ENABLED=true</span> and
            <span className="font-mono"> POLYGON_API_KEY</span>.
          </p>

          {financeActionStatus && (
            <div
              className={`rounded-md border px-3 py-2 text-sm ${
                financeActionStatus.type === 'error'
                  ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-200'
                  : 'border-green-200 bg-green-50 text-green-700 dark:border-green-900/50 dark:bg-green-950/40 dark:text-green-200'
              }`}
            >
              {financeActionStatus.message}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <CardTitle>Finance Stream</CardTitle>
            <CardDescription>Latest market ticks, predictions, and anomaly scores</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="max-h-96 space-y-4 overflow-y-auto">
              {financeData.length > 0 ? (
                [...financeData].reverse().map((item, index) => (
                  <div
                    key={`${item.symbol || 'finance'}-${item.createdAt || item.eventTimestamp || index}`}
                    className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-900"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <div className="font-semibold">{item.symbol}</div>
                        <div className="text-sm text-gray-500">{item.provider}</div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">{item.marketSegment || 'FINANCE'}</Badge>
                        <Badge variant={getFinanceSignalVariant(item.trendSignal)}>{item.trendSignal || 'NEUTRAL'}</Badge>
                      </div>
                    </div>
                    <div className="grid gap-2 text-sm text-gray-600 dark:text-gray-300 md:grid-cols-2">
                      <div>Current: {formatCurrency(item.currentPrice)}</div>
                      <div>Predicted: {formatCurrency(item.predictedPrice)}</div>
                      <div>Move: {toNumber(item.predictedChangePercentage)?.toFixed(2) || '0.00'}%</div>
                      <div>Anomaly: {toNumber(item.anomalyScore)?.toFixed(2) || '0.00'}</div>
                      <div>Volume: {formatCompactNumber(item.totalVolume)}</div>
                      <div>Market Cap: {formatCompactNumber(item.marketCap)}</div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="py-8 text-center text-gray-500">Waiting for finance stream data...</div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Latest Prediction</CardTitle>
            <CardDescription>Most recent finance inference</CardDescription>
          </CardHeader>
          <CardContent>
            {realtimeFinance ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-2xl font-bold">{realtimeFinance.symbol}</div>
                    <div className="text-sm text-gray-500">{realtimeFinance.provider}</div>
                  </div>
                  <Badge variant={getFinanceSignalVariant(realtimeFinance.trendSignal)}>
                    {realtimeFinance.trendSignal || 'NEUTRAL'}
                  </Badge>
                </div>
                <div className="space-y-2">
                  <div className="text-sm text-gray-500">Current price</div>
                  <div className="text-3xl font-bold text-amber-500">{formatCurrency(realtimeFinance.currentPrice)}</div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div className="rounded-lg bg-slate-50 p-3 dark:bg-slate-900">
                    <div className="text-gray-500">Predicted</div>
                    <div className="font-semibold">{formatCurrency(realtimeFinance.predictedPrice)}</div>
                  </div>
                  <div className="rounded-lg bg-slate-50 p-3 dark:bg-slate-900">
                    <div className="text-gray-500">Confidence</div>
                    <div className="font-semibold">
                      {Math.round((toNumber(realtimeFinance.predictionConfidence) || 0) * 100)}%
                    </div>
                  </div>
                </div>
                <div className="rounded-lg border border-slate-200 p-3 text-sm dark:border-slate-800">
                  <div className={`font-medium ${getFinanceSignalColorClass(realtimeFinance.trendSignal)}`}>
                    {realtimeFinance.insight}
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-8 text-center text-gray-500">No finance prediction available yet</div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-blue-600" />
              <span>Live Finance Trend</span>
            </CardTitle>
            <CardDescription>
              Current versus predicted price for {selectedFinanceSymbol || 'the latest tracked asset'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {financeTrendData.length > 0 ? (
              <ResponsiveContainer width="100%" height={320}>
                <LineChart data={financeTrendData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="currentPrice" stroke="#f59e0b" strokeWidth={2} connectNulls />
                  <Line type="monotone" dataKey="predictedPrice" stroke="#2563eb" strokeWidth={2} connectNulls />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-[320px] items-center justify-center text-center text-gray-500">
                Waiting for finance chart data...
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Finance Signals</CardTitle>
            <CardDescription>Recent insights from the streaming finance model</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {financeData.length > 0 ? (
                [...financeData]
                  .slice(-5)
                  .reverse()
                  .map((item, index) => (
                    <div key={`${item.symbol || 'signal'}-${item.createdAt || index}`} className="rounded-lg border p-4">
                      <div className="flex items-center justify-between gap-3">
                        <div className="font-semibold">{item.symbol}</div>
                        <Badge variant={getFinanceSignalVariant(item.trendSignal)}>{item.trendSignal || 'NEUTRAL'}</Badge>
                      </div>
                      <div className="mt-2 text-sm text-gray-500">
                        Confidence {Math.round((toNumber(item.predictionConfidence) || 0) * 100)}% and anomaly score{' '}
                        {toNumber(item.anomalyScore)?.toFixed(2) || '0.00'}
                      </div>
                      <div className={`mt-3 text-sm ${getFinanceSignalColorClass(item.trendSignal)}`}>{item.insight}</div>
                    </div>
                  ))
              ) : (
                <div className="py-8 text-center text-gray-500">No finance insights available yet</div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  )
}

export default FinanceTab
