import { Activity, Droplets, Eye, Sun, Thermometer, Wind } from 'lucide-react'

function SensorTypeIcon({ sensorType, className = 'h-4 w-4' }) {
  switch (sensorType) {
    case 'temperature':
      return <Thermometer className={className} />
    case 'humidity':
      return <Droplets className={className} />
    case 'pressure':
      return <Wind className={className} />
    case 'light':
      return <Sun className={className} />
    case 'motion':
      return <Eye className={className} />
    case 'air-quality':
      return <Wind className={className} />
    default:
      return <Activity className={className} />
  }
}

export default SensorTypeIcon
