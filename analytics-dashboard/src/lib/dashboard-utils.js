const SENSOR_DISTRIBUTION_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4']

export function normalizeTimestamp(value) {
  if (typeof value !== 'string') {
    return value
  }

  return value.replace(/(\.\d{3})\d+/, '$1')
}

export function toDate(value) {
  if (!value) {
    return null
  }

  const parsedDate = new Date(normalizeTimestamp(value))
  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate
}

export function toNumber(value) {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : null
}

export function formatChartTime(timestamp, fallbackLabel) {
  const parsedDate = toDate(timestamp)
  if (!parsedDate) {
    return fallbackLabel
  }

  return parsedDate.toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatLastUpdated(timestamp) {
  const parsedDate = toDate(timestamp)
  return parsedDate ? parsedDate.toLocaleTimeString() : 'Waiting for live data'
}

export function formatCurrency(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: Number(value) < 1 ? 4 : 2,
  }).format(Number(value))
}

export function formatCompactNumber(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }

  return new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 2,
  }).format(Number(value))
}

export function formatSensorLabel(sensorType) {
  if (!sensorType) {
    return 'Unknown'
  }

  return sensorType
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function buildSensorTrendData(iotEvents = []) {
  return iotEvents.slice(-20).map((event, index) => ({
    time: formatChartTime(event.timestamp, `Sample ${index + 1}`),
    temperature: event.sensorType === 'temperature' ? toNumber(event.sensorValue) : null,
    humidity: event.sensorType === 'humidity' ? toNumber(event.sensorValue) : null,
    anomalyScore: toNumber(event.anomalyScore),
  }))
}

export function buildSentimentTrendData(socialEvents = []) {
  return socialEvents.slice(-20).map((event, index) => ({
    time: formatChartTime(event.timestamp || event.postTimestamp, `Post ${index + 1}`),
    sentiment: toNumber(event.sentimentScore),
    engagement: toNumber(event.engagementRate),
  }))
}

export function buildSensorDistribution(iotEvents = []) {
  const countsBySensorType = iotEvents.reduce((accumulator, event) => {
    if (!event.sensorType) {
      return accumulator
    }

    accumulator[event.sensorType] = (accumulator[event.sensorType] || 0) + 1
    return accumulator
  }, {})

  return Object.entries(countsBySensorType).map(([sensorType, count], index) => ({
    name: formatSensorLabel(sensorType),
    value: count,
    fill: SENSOR_DISTRIBUTION_COLORS[index % SENSOR_DISTRIBUTION_COLORS.length],
  }))
}

export function createSampleIoTPayload() {
  const timestamp = new Date().toISOString()
  const samples = [
    {
      deviceId: 'TEMP_001',
      sensorType: 'temperature',
      sensorValue: 20 + Math.random() * 12,
      unit: 'C',
      location: 'Building A - Floor 1',
    },
    {
      deviceId: 'HUM_001',
      sensorType: 'humidity',
      sensorValue: 35 + Math.random() * 35,
      unit: '%',
      location: 'Server Room',
    },
    {
      deviceId: 'LIGHT_001',
      sensorType: 'light',
      sensorValue: 250 + Math.random() * 400,
      unit: 'lux',
      location: 'Lobby',
    },
  ]

  const sample = samples[Math.floor(Math.random() * samples.length)]

  return {
    ...sample,
    timestamp,
    metadata: {
      source: 'dashboard',
      mode: 'manual-test',
    },
  }
}

export function createSampleSocialPayload() {
  const timestamp = new Date().toISOString()
  const samples = [
    {
      platform: 'twitter',
      content: 'Smart building sensors are showing stable readings this evening #iot #analytics',
      likesCount: 18,
      sharesCount: 6,
      commentsCount: 3,
      followersCount: 1200,
    },
    {
      platform: 'instagram',
      content: 'Air quality data looks strong today #environment #health',
      likesCount: 42,
      sharesCount: 11,
      commentsCount: 8,
      followersCount: 2800,
    },
    {
      platform: 'linkedin',
      content: 'Real-time stream processing helps teams react faster to live operational changes #kafka #realtime',
      likesCount: 25,
      sharesCount: 9,
      commentsCount: 4,
      followersCount: 2100,
    },
  ]

  const sample = samples[Math.floor(Math.random() * samples.length)]

  return {
    ...sample,
    postId: `manual_${Date.now()}`,
    userId: 'dashboard_user',
    username: '@dashboard',
    postTimestamp: timestamp,
  }
}

export function getSentimentColorClass(score) {
  if (score > 0.2) return 'text-green-600'
  if (score < -0.2) return 'text-red-600'
  return 'text-yellow-600'
}

export function getAlertSeverityVariant(severity) {
  switch (severity) {
    case 'HIGH':
      return 'destructive'
    case 'MEDIUM':
      return 'default'
    case 'LOW':
      return 'secondary'
    default:
      return 'default'
  }
}

export function getFinanceSignalVariant(signal) {
  switch (signal) {
    case 'BULLISH':
      return 'default'
    case 'BEARISH':
      return 'destructive'
    case 'VOLATILE':
      return 'secondary'
    default:
      return 'outline'
  }
}

export function getFinanceSignalColorClass(signal) {
  switch (signal) {
    case 'BULLISH':
      return 'text-green-600'
    case 'BEARISH':
      return 'text-red-600'
    case 'VOLATILE':
      return 'text-amber-600'
    default:
      return 'text-gray-500'
  }
}
