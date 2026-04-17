# Real-time Data Analytics Application

A comprehensive real-time data analytics platform built with Java Spring Boot and React, featuring machine learning integration for IoT sensor data and social media analytics youtube etc, finance data analytics.

## 🚀 Features

- **Real-time Data Ingestion**: Process IoT sensor data and social media feeds in real-time
- **Machine Learning Integration**: Anomaly detection, sentiment analysis, and trend prediction
- **Interactive Dashboard**: Modern React-based dashboard with real-time visualizations
- **Stream Processing**: Apache Kafka-based event streaming architecture
- **WebSocket Communication**: Real-time updates and notifications
- **Scalable Architecture**: Microservices-ready design with horizontal scaling support

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   IoT Devices   │    │ Social Media    │    │   External      │
│   & Sensors     │    │   Platforms     │    │   Systems       │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │     Data Ingestion        │
                    │    (Spring Boot API)      │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │      Apache Kafka         │
                    │   (Message Streaming)     │
                    └─────────────┬─────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
┌─────────▼─────────┐   ┌─────────▼─────────┐   ┌─────────▼─────────┐
│   ML Processing   │   │ Stream Processing │   │   Data Storage    │
│  (Deeplearning4j) │   │   & Analytics     │   │   (MySQL/H2)     │
└─────────┬─────────┘   └─────────┬─────────┘   └─────────┬─────────┘
          │                       │                       │
          └───────────────────────┼───────────────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │     WebSocket Server      │
                    │   (Real-time Updates)     │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │    React Dashboard        │
                    │  (Interactive Frontend)  │
                    └───────────────────────────┘
```

## 🛠️ Technology Stack

### Backend
- **Java 11+** - Core programming language
- **Spring Boot 2.7** - Application framework
- **Spring Data JPA** - Data access layer
- **Apache Kafka** - Message streaming platform
- **Deeplearning4j** - Machine learning framework
- **WebSocket** - Real-time communication
- **MySQL/H2** - Database systems
- **Maven** - Dependency management

### Frontend
- **React 18** - UI framework
- **Tailwind CSS** - Styling framework
- **Recharts** - Data visualization
- **WebSocket/STOMP** - Real-time communication
- **Vite** - Build tool
- **shadcn/ui** - UI components

### Infrastructure
- **Docker** - Containerization
- **Kafka** - Event streaming
- **WebSocket** - Real-time messaging

## 📋 Prerequisites

- Java 11 or higher
- Node.js 18 or higher
- Maven 3.6+
- Docker (optional)
- MySQL 8.0+ (for production)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd realtime-analytics-app
```

### 2. Backend Setup

```bash
# Navigate to backend directory
cd realtime-analytics-app

# Install dependencies and run tests
mvn clean install

# Run the application
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup

```bash
# Navigate to frontend directory
cd analytics-dashboard

# Install dependencies
pnpm install

# Start development server
pnpm run dev
```

The frontend will start on `http://localhost:5173`

### 4. Access the Application

Open your browser and navigate to `http://localhost:5173` to access the dashboard.

## 📊 API Endpoints

### Data Ingestion

#### IoT Sensor Data
```http
POST /api/data/iot/sensor
Content-Type: application/json

{
  "deviceId": "TEMP_001",
  "sensorType": "temperature",
  "sensorValue": 22.5,
  "unit": "°C",
  "location": "Building A - Floor 1",
  "timestamp": "2024-12-07T10:30:00Z"
}
```

#### Social Media Data
```http
POST /api/data/social-media
Content-Type: application/json

{
  "platform": "twitter",
  "postId": "tweet_123",
  "content": "Great IoT technology! #innovation #tech",
  "userId": "user_456",
  "username": "@techuser",
  "likesCount": 10,
  "sharesCount": 5,
  "commentsCount": 2
}
```

### Analytics

#### Get Statistics
```http
GET /api/data/stats
```

#### Health Check
```http
GET /api/data/health
```

## 🔧 Configuration

### Backend Configuration (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: analytics-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

logging:
  level:
    com.analytics.app: DEBUG
```

### Frontend Configuration

Environment variables can be set in `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws-native
```

## 🧪 Testing

### Backend Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DataIngestionServiceTest

# Run with coverage
mvn test jacoco:report
```

### Frontend Tests

```bash
# Run tests
pnpm test

# Run tests with coverage
pnpm test:coverage
```

## 📦 Docker Deployment

### Using Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Individual Container Build

```bash
# Backend
cd realtime-analytics-app
docker build -t analytics-backend .

# Frontend
cd analytics-dashboard
docker build -t analytics-frontend .
```

## 🔍 Monitoring and Observability

### Health Endpoints

- Application Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Info: `GET /actuator/info`

### Logging

The application uses structured logging with configurable levels:

```yaml
logging:
  level:
    com.analytics.app: INFO
    org.springframework.kafka: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

## 🚀 Production Deployment

### Environment Setup

1. **Database Setup**: Configure MySQL for production
2. **Kafka Cluster**: Set up multi-node Kafka cluster
3. **Load Balancer**: Configure for high availability
4. **Monitoring**: Set up application monitoring

### Security Considerations

- Enable HTTPS/TLS encryption
- Configure authentication and authorization
- Set up API rate limiting
- Implement input validation and sanitization
- Configure CORS policies

## 📈 Performance Optimization

### Backend Optimizations

- Connection pooling configuration
- JVM tuning parameters
- Database indexing strategies
- Caching implementation

### Frontend Optimizations

- Code splitting and lazy loading
- Asset compression and minification
- CDN integration for static assets
- Service worker for offline capabilities

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Apache Kafka community for the streaming platform
- Deeplearning4j team for the ML framework
- React team for the frontend framework
- All open-source contributors

## 📞 Support

## 🔑 Required Environment Variables (Windows PowerShell)

$env:YOUTUBE_API_KEY="your_key"
$env:ANALYTICS_FINANCE_ADMIN_API_KEY="your_admin_key"
$env:YOUTUBE_API_ENABLED="true"

For support and questions:

- Create an issue in the repository
- Check the [documentation](PROJECT_DOCUMENTATION.md)
- Review the [API documentation](#api-endpoints)

## 🗺️ Roadmap

- [ ] Advanced ML model integration
- [ ] Enhanced data source connectors
- [ ] Mobile application development
- [ ] Cloud-native deployment options
- [ ] Advanced visualization features
- [ ] Real-time collaboration features

---
## 🎥 Demo Video

▶️ [Watch Demo Video on YouTube](https://youtu.be/DQvxunWNptU)

