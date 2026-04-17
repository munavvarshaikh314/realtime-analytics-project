package com.analytics.app.service;

import com.analytics.app.model.IoTSensorData;
import com.analytics.app.model.SocialMediaData;
import com.analytics.app.model.YouTubeTrendingVideo;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MLProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MLProcessingService.class);
    
    // ML Models
    private MultiLayerNetwork iotAnomalyModel;
    private MultiLayerNetwork timeSeriesForecaster;
    private Map<String, Double> sensorBaselines;
    
    // Enhanced sentiment analysis
    private Map<String, Double> positiveWords;
    private Map<String, Double> negativeWords;
    private Map<String, Double> intensifiers;
    private Map<String, Double> negations;
    
    // Trending detection
    private Map<String, Integer> hashtagFrequency;
    private Map<String, Double> trendingThresholds;
    
    // Time-series data storage
    private Map<String, List<Double>> timeSeriesData;
    private Map<String, List<Long>> timeSeriesTimestamps;
    
    @PostConstruct
    public void initializeModels() {
        logger.info("Initializing ML models...");
        
        initializeIoTAnomalyModel();
        initializeTimeSeriesForecaster();
        initializeEnhancedSentimentAnalysis();
        initializeTrendingDetection();
        initializeTimeSeriesStorage();
        
        logger.info("ML models initialized successfully");
    }
    
    /**
     * Initialize IoT anomaly detection model
     */
    private void initializeIoTAnomalyModel() {
        // Create a simple autoencoder for anomaly detection
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(5) // Input features: sensor value, time of day, day of week, device age, location
                        .nOut(3)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nIn(3)
                        .nOut(2)
                        .activation(Activation.RELU)
                        .build())
                .layer(2, new DenseLayer.Builder()
                        .nIn(2)
                        .nOut(3)
                        .activation(Activation.RELU)
                        .build())
                .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(3)
                        .nOut(5)
                        .activation(Activation.IDENTITY)
                        .build())
                .build();
        
        iotAnomalyModel = new MultiLayerNetwork(conf);
        iotAnomalyModel.init();
        
        // Initialize sensor baselines
        sensorBaselines = new HashMap<>();
        sensorBaselines.put("temperature", 22.0);
        sensorBaselines.put("humidity", 45.0);
        sensorBaselines.put("pressure", 1013.25);
        sensorBaselines.put("light", 500.0);
        sensorBaselines.put("motion", 0.0);
        sensorBaselines.put("air-quality", 50.0);
        
        logger.info("IoT anomaly detection model initialized");
    }
    
    /**
     * Initialize sentiment analysis dictionaries
     */
    private void initializeSentimentAnalysis() {
        positiveWords = new HashMap<>();
        negativeWords = new HashMap<>();
        
        // Positive sentiment words with weights
        positiveWords.put("good", 1.0);
        positiveWords.put("great", 1.5);
        positiveWords.put("excellent", 2.0);
        positiveWords.put("amazing", 2.0);
        positiveWords.put("awesome", 1.8);
        positiveWords.put("fantastic", 2.0);
        positiveWords.put("wonderful", 1.8);
        positiveWords.put("perfect", 2.0);
        positiveWords.put("love", 1.5);
        positiveWords.put("like", 1.0);
        positiveWords.put("happy", 1.5);
        positiveWords.put("excited", 1.8);
        positiveWords.put("thrilled", 2.0);
        positiveWords.put("pleased", 1.2);
        positiveWords.put("satisfied", 1.3);
        positiveWords.put("delighted", 1.8);
        positiveWords.put("impressed", 1.5);
        positiveWords.put("outstanding", 2.0);
        positiveWords.put("brilliant", 1.8);
        positiveWords.put("superb", 1.8);
        
        // Negative sentiment words with weights
        negativeWords.put("bad", -1.0);
        negativeWords.put("terrible", -2.0);
        negativeWords.put("awful", -2.0);
        negativeWords.put("horrible", -2.0);
        negativeWords.put("hate", -1.8);
        negativeWords.put("dislike", -1.2);
        negativeWords.put("angry", -1.5);
        negativeWords.put("frustrated", -1.5);
        negativeWords.put("disappointed", -1.3);
        negativeWords.put("sad", -1.2);
        negativeWords.put("upset", -1.3);
        negativeWords.put("annoyed", -1.2);
        negativeWords.put("disgusted", -1.8);
        negativeWords.put("furious", -2.0);
        negativeWords.put("outraged", -2.0);
        negativeWords.put("pathetic", -1.8);
        negativeWords.put("useless", -1.5);
        negativeWords.put("worthless", -1.8);
        negativeWords.put("stupid", -1.5);
        negativeWords.put("ridiculous", -1.3);
        
        logger.info("Sentiment analysis dictionaries initialized");
    }
    
    /**
     * Initialize trending detection
     */
    private void initializeTrendingDetection() {
        hashtagFrequency = new HashMap<>();
        trendingThresholds = new HashMap<>();
        
        // Platform-specific trending thresholds
        trendingThresholds.put("twitter", 100.0);
        trendingThresholds.put("facebook", 50.0);
        trendingThresholds.put("instagram", 75.0);
        trendingThresholds.put("linkedin", 25.0);
        trendingThresholds.put("youtube", 120.0);
        trendingThresholds.put("reddit", 40.0);
        
        logger.info("Trending detection initialized");
    }
    
    /**
     * Initialize time-series forecasting model
     */
    private void initializeTimeSeriesForecaster() {
        // Simple LSTM-like network for time-series forecasting
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(456)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(10) // Last 10 time steps
                        .nOut(5)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nIn(5)
                        .nOut(3)
                        .activation(Activation.TANH)
                        .build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(3)
                        .nOut(1) // Predict next value
                        .activation(Activation.IDENTITY)
                        .build())
                .build();
        
        timeSeriesForecaster = new MultiLayerNetwork(conf);
        timeSeriesForecaster.init();
        
        logger.info("Time-series forecasting model initialized");
    }
    
    /**
     * Initialize enhanced sentiment analysis with intensifiers and negations
     */
    private void initializeEnhancedSentimentAnalysis() {
        positiveWords = new HashMap<>();
        negativeWords = new HashMap<>();
        intensifiers = new HashMap<>();
        negations = new HashMap<>();
        
        // Positive sentiment words with weights
        positiveWords.put("good", 1.0);
        positiveWords.put("great", 1.5);
        positiveWords.put("excellent", 2.0);
        positiveWords.put("amazing", 2.0);
        positiveWords.put("awesome", 1.8);
        positiveWords.put("fantastic", 2.0);
        positiveWords.put("wonderful", 1.8);
        positiveWords.put("perfect", 2.0);
        positiveWords.put("love", 1.5);
        positiveWords.put("like", 1.0);
        positiveWords.put("happy", 1.5);
        positiveWords.put("excited", 1.8);
        positiveWords.put("thrilled", 2.0);
        positiveWords.put("pleased", 1.2);
        positiveWords.put("satisfied", 1.3);
        positiveWords.put("delighted", 1.8);
        positiveWords.put("impressed", 1.5);
        positiveWords.put("outstanding", 2.0);
        positiveWords.put("brilliant", 1.8);
        positiveWords.put("superb", 1.8);
        
        // Negative sentiment words with weights
        negativeWords.put("bad", -1.0);
        negativeWords.put("terrible", -2.0);
        negativeWords.put("awful", -2.0);
        negativeWords.put("horrible", -2.0);
        negativeWords.put("hate", -1.8);
        negativeWords.put("dislike", -1.2);
        negativeWords.put("angry", -1.5);
        negativeWords.put("frustrated", -1.5);
        negativeWords.put("disappointed", -1.3);
        negativeWords.put("sad", -1.2);
        negativeWords.put("upset", -1.3);
        negativeWords.put("annoyed", -1.2);
        negativeWords.put("disgusted", -1.8);
        negativeWords.put("furious", -2.0);
        negativeWords.put("outraged", -2.0);
        negativeWords.put("pathetic", -1.8);
        negativeWords.put("useless", -1.5);
        negativeWords.put("worthless", -1.8);
        negativeWords.put("stupid", -1.5);
        negativeWords.put("ridiculous", -1.3);
        
        // Intensifiers
        intensifiers.put("very", 1.5);
        intensifiers.put("really", 1.5);
        intensifiers.put("extremely", 2.0);
        intensifiers.put("absolutely", 2.0);
        intensifiers.put("totally", 1.8);
        intensifiers.put("completely", 1.8);
        intensifiers.put("so", 1.3);
        intensifiers.put("incredibly", 2.0);
        intensifiers.put("highly", 1.8);
        
        // Negations
        negations.put("not", -1.0);
        negations.put("never", -1.0);
        negations.put("no", -1.0);
        negations.put("n't", -1.0);
        negations.put("cannot", -1.0);
        negations.put("can't", -1.0);
        negations.put("won't", -1.0);
        negations.put("don't", -1.0);
        negations.put("doesn't", -1.0);
        negations.put("isn't", -1.0);
        negations.put("aren't", -1.0);
        
        logger.info("Enhanced sentiment analysis initialized");
    }
    
    /**
     * Initialize time-series data storage
     */
    private void initializeTimeSeriesStorage() {
        timeSeriesData = new HashMap<>();
        timeSeriesTimestamps = new HashMap<>();
        
        // Initialize storage for different metrics
        timeSeriesData.put("iot_temperature", new ArrayList<>());
        timeSeriesData.put("iot_humidity", new ArrayList<>());
        timeSeriesData.put("social_sentiment", new ArrayList<>());
        timeSeriesData.put("finance_price", new ArrayList<>());
        
        timeSeriesTimestamps.put("iot_temperature", new ArrayList<>());
        timeSeriesTimestamps.put("iot_humidity", new ArrayList<>());
        timeSeriesTimestamps.put("social_sentiment", new ArrayList<>());
        timeSeriesTimestamps.put("finance_price", new ArrayList<>());
        
        logger.info("Time-series storage initialized");
    }
    
    /**
     * Detect anomalies in IoT sensor data
     */
    public void detectIoTAnomalies(IoTSensorData sensorData) {
        try {
            // Prepare input features
            INDArray input = prepareIoTFeatures(sensorData);
            
            // Get model prediction (reconstruction)
            INDArray output = iotAnomalyModel.output(input);
            
            // Calculate reconstruction error
            INDArray error = input.sub(output);
            double reconstructionError = error.norm2Number().doubleValue();
            
            // Determine anomaly threshold based on sensor type
            double threshold = getAnomalyThreshold(sensorData.getSensorType());
            
            // Set anomaly flag and score
            boolean isAnomaly = reconstructionError > threshold;
            sensorData.setIsAnomaly(isAnomaly);
            sensorData.setAnomalyScore(reconstructionError);
            
            // Store data for time-series analysis
            String metricKey = "iot_" + sensorData.getSensorType().toLowerCase();
            addTimeSeriesData(metricKey, sensorData.getSensorValue(), 
                            sensorData.getTimestamp().toEpochSecond(ZoneOffset.UTC) * 1000);
            
            // Check for time-series anomalies
            boolean timeSeriesAnomaly = detectTimeSeriesAnomaly(metricKey, sensorData.getSensorValue());
            if (timeSeriesAnomaly && !isAnomaly) {
                sensorData.setIsAnomaly(true);
                sensorData.setAnomalyScore(Math.max(reconstructionError, 1.0));
                logger.warn("Time-series anomaly detected for IoT data: Device={}, Type={}, Value={}", 
                           sensorData.getDeviceId(), sensorData.getSensorType(), sensorData.getSensorValue());
            }
            
            if (isAnomaly) {
                logger.warn("Anomaly detected in IoT data: Device={}, Type={}, Value={}, Score={}", 
                           sensorData.getDeviceId(), sensorData.getSensorType(), 
                           sensorData.getSensorValue(), reconstructionError);
            }
            
        } catch (Exception e) {
            logger.error("Error in IoT anomaly detection: {}", e.getMessage());
            sensorData.setIsAnomaly(false);
            sensorData.setAnomalyScore(0.0);
        }
    }
    
    /**
     * Analyze sentiment of social media data with enhanced algorithm
     */
    public void analyzeSentiment(SocialMediaData socialData) {
        try {
            String content = socialData.getContent().toLowerCase();
            
            // Remove URLs, mentions, and special characters
            content = cleanText(content);
            
            // Calculate enhanced sentiment score
            double sentimentScore = calculateEnhancedSentimentScore(content);
            
            // Normalize sentiment score to [-1, 1] range
            sentimentScore = Math.max(-1.0, Math.min(1.0, sentimentScore));
            
            // Determine sentiment label and emotion
            String sentimentLabel;
            String emotion;
            
            if (sentimentScore > 0.2) {
                sentimentLabel = "POSITIVE";
                emotion = sentimentScore > 0.7 ? "JOY" : 
                         sentimentScore > 0.5 ? "SATISFACTION" : "CONTENTMENT";
            } else if (sentimentScore < -0.2) {
                sentimentLabel = "NEGATIVE";
                emotion = sentimentScore < -0.7 ? "ANGER" : 
                         sentimentScore < -0.5 ? "FRUSTRATION" : "DISAPPOINTMENT";
            } else {
                sentimentLabel = "NEUTRAL";
                emotion = "NEUTRAL";
            }
            
            socialData.setSentimentScore(sentimentScore);
            socialData.setSentimentLabel(sentimentLabel);
            socialData.setEmotion(emotion);
            
            // Store sentiment data for time-series analysis
            addTimeSeriesData("social_sentiment", sentimentScore, System.currentTimeMillis());
            
            logger.debug("Enhanced sentiment analysis completed: Platform={}, Score={}, Label={}, Emotion={}", 
                        socialData.getPlatform(), sentimentScore, sentimentLabel, emotion);
            
        } catch (Exception e) {
            logger.error("Error in enhanced sentiment analysis: {}", e.getMessage());
            socialData.setSentimentScore(0.0);
            socialData.setSentimentLabel("NEUTRAL");
            socialData.setEmotion("NEUTRAL");
        }
    }
    
    /**
     * Detect trending topics and posts
     */
    public void detectTrends(SocialMediaData socialData) {
        try {
            // Update hashtag frequency
            if (socialData.getHashtags() != null) {
                for (String hashtag : socialData.getHashtags()) {
                    hashtagFrequency.merge(hashtag.toLowerCase(), 1, Integer::sum);
                }
            }
            
            // Calculate trending score based on engagement metrics
            double trendingScore = calculateTrendingScore(socialData);
            
            // Get platform-specific threshold
            double threshold = trendingThresholds.getOrDefault(socialData.getPlatform().toLowerCase(), 50.0);
            
            // Determine if post is trending
            boolean isTrending = trendingScore > threshold;
            socialData.setIsTrending(isTrending);
            
            // Calculate influence score
            double influenceScore = calculateInfluenceScore(socialData);
            socialData.setInfluenceScore(influenceScore);
            
            if (isTrending) {
                logger.info("Trending post detected: Platform={}, PostId={}, Score={}", 
                           socialData.getPlatform(), socialData.getPostId(), trendingScore);
            }
            
        } catch (Exception e) {
            logger.error("Error in trend detection: {}", e.getMessage());
            socialData.setIsTrending(false);
            socialData.setInfluenceScore(0.0);
        }
    }
    
    /**
     * Prepare IoT features for ML model
     */
    private INDArray prepareIoTFeatures(IoTSensorData sensorData) {
        double[] features = new double[5];
        
        // Feature 1: Normalized sensor value
        Double baseline = sensorBaselines.get(sensorData.getSensorType().toLowerCase());
        if (baseline != null) {
            features[0] = sensorData.getSensorValue() / baseline;
        } else {
            features[0] = sensorData.getSensorValue();
        }
        
        // Feature 2: Time of day (0-1)
        int hour = sensorData.getTimestamp().getHour();
        features[1] = hour / 24.0;
        
        // Feature 3: Day of week (0-1)
        int dayOfWeek = sensorData.getTimestamp().getDayOfWeek().getValue();
        features[2] = dayOfWeek / 7.0;
        
        // Feature 4: Device age simulation (random for demo)
        features[3] = Math.random();
        
        // Feature 5: Location hash (simplified)
        if (sensorData.getLocation() != null) {
            features[4] = Math.abs(sensorData.getLocation().hashCode()) % 100 / 100.0;
        } else {
            features[4] = 0.5;
        }
        
        return Nd4j.create(features).reshape(1, 5);
    }
    
    /**
     * Get anomaly threshold for sensor type
     */
    private double getAnomalyThreshold(String sensorType) {
        switch (sensorType.toLowerCase()) {
            case "temperature": return 0.5;
            case "humidity": return 0.3;
            case "pressure": return 0.4;
            case "light": return 0.6;
            case "motion": return 0.2;
            case "air-quality": return 0.4;
            default: return 0.5;
        }
    }
    
    /**
     * Clean text for sentiment analysis
     */
    private String cleanText(String text) {
        // Remove URLs
        text = text.replaceAll("http[s]?://\\S+", "");
        
        // Remove mentions
        text = text.replaceAll("@\\w+", "");
        
        // Remove special characters except spaces
        text = text.replaceAll("[^a-zA-Z\\s]", "");
        
        // Remove extra spaces
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
    
    /**
     * Calculate sentiment score from text
     */
    private double calculateSentimentScore(String text) {
        String[] words = text.split("\\s+");
        double totalScore = 0.0;
        int wordCount = 0;
        
        for (String word : words) {
            word = word.trim().toLowerCase();
            if (word.length() > 2) { // Ignore very short words
                if (positiveWords.containsKey(word)) {
                    totalScore += positiveWords.get(word);
                    wordCount++;
                } else if (negativeWords.containsKey(word)) {
                    totalScore += negativeWords.get(word);
                    wordCount++;
                }
            }
        }
        
        // Normalize by word count
        if (wordCount > 0) {
            return totalScore / wordCount;
        } else {
            return 0.0;
        }
    }
    
    /**
     * Calculate trending score for social media post
     */
    private double calculateTrendingScore(SocialMediaData socialData) {
        double score = 0.0;
        
        // Engagement metrics
        if (socialData.getLikesCount() != null) {
            score += socialData.getLikesCount() * 1.0;
        }
        if (socialData.getSharesCount() != null) {
            score += socialData.getSharesCount() * 3.0; // Shares are more valuable
        }
        if (socialData.getCommentsCount() != null) {
            score += socialData.getCommentsCount() * 2.0;
        }
        
        // Hashtag popularity boost
        if (socialData.getHashtags() != null) {
            for (String hashtag : socialData.getHashtags()) {
                Integer frequency = hashtagFrequency.get(hashtag.toLowerCase());
                if (frequency != null && frequency > 10) {
                    score += Math.log(frequency) * 5.0;
                }
            }
        }
        
        // Follower count influence
        if (socialData.getFollowersCount() != null && socialData.getFollowersCount() > 0) {
            score += Math.log(socialData.getFollowersCount()) * 2.0;
        }
        
        return score;
    }
    
    /**
     * Calculate influence score for user/post
     */
    private double calculateInfluenceScore(SocialMediaData socialData) {
        double score = 0.0;
        
        // Base score from follower count
        if (socialData.getFollowersCount() != null && socialData.getFollowersCount() > 0) {
            score = Math.log10(socialData.getFollowersCount()) * 10.0;
        }
        
        // Engagement rate boost
        if (socialData.getEngagementRate() != null) {
            score += socialData.getEngagementRate() * 5.0;
        }
        
        // Sentiment boost for positive content
        if (socialData.getSentimentScore() != null && socialData.getSentimentScore() > 0.5) {
            score += socialData.getSentimentScore() * 10.0;
        }
        
        // Virality boost
        if (socialData.getViralityScore() != null) {
            score += socialData.getViralityScore() * 0.1;
        }
        
        return Math.min(100.0, score); // Cap at 100
    }
    
    /**
     * Train IoT anomaly model with historical data
     */
    public void trainIoTAnomalyModel(List<IoTSensorData> trainingData) {
        logger.info("Training IoT anomaly detection model with {} samples", trainingData.size());
        
        if (trainingData.size() < 10) {
            logger.warn("Insufficient training data for IoT model");
            return;
        }
        
        try {
            // Prepare training data
            INDArray features = Nd4j.zeros(trainingData.size(), 5);
            
            for (int i = 0; i < trainingData.size(); i++) {
                INDArray sample = prepareIoTFeatures(trainingData.get(i));
                features.putRow(i, sample);
            }
            
            // Train autoencoder (self-supervised)
            for (int epoch = 0; epoch < 100; epoch++) {
                iotAnomalyModel.fit(features, features);
            }
            
            logger.info("IoT anomaly detection model training completed");
            
        } catch (Exception e) {
            logger.error("Error training IoT anomaly model: {}", e.getMessage());
        }
    }
    
    /**
     * Update sentiment analysis model with new data
     */
    public void updateSentimentModel(List<SocialMediaData> trainingData) {
        logger.info("Updating sentiment analysis model with {} samples", trainingData.size());
        
        // Update word weights based on labeled data
        Map<String, List<Double>> wordScores = new HashMap<>();
        
        for (SocialMediaData data : trainingData) {
            if (data.getSentimentScore() != null && data.getContent() != null) {
                String[] words = cleanText(data.getContent().toLowerCase()).split("\\s+");
                
                for (String word : words) {
                    if (word.length() > 2) {
                        wordScores.computeIfAbsent(word, k -> new ArrayList<>())
                                 .add(data.getSentimentScore());
                    }
                }
            }
        }
        
        // Update word dictionaries with average scores
        for (Map.Entry<String, List<Double>> entry : wordScores.entrySet()) {
            String word = entry.getKey();
            List<Double> scores = entry.getValue();
            
            if (scores.size() >= 3) { // Minimum occurrences
                double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                
                if (avgScore > 0.3) {
                    positiveWords.put(word, avgScore);
                    negativeWords.remove(word);
                } else if (avgScore < -0.3) {
                    negativeWords.put(word, avgScore);
                    positiveWords.remove(word);
                }
            }
        }
        
        logger.info("Sentiment analysis model updated");
    }
    
    /**
     * Calculate enhanced sentiment score with intensifiers and negations
     */
    private double calculateEnhancedSentimentScore(String text) {
        String[] words = text.split("\\s+");
        double totalScore = 0.0;
        int wordCount = 0;
        double intensifierMultiplier = 1.0;
        boolean negationActive = false;
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i].trim().toLowerCase();
            
            if (word.length() <= 2) continue; // Skip very short words
            
            // Check for negations
            if (negations.containsKey(word)) {
                negationActive = true;
                continue;
            }
            
            // Check for intensifiers
            if (intensifiers.containsKey(word)) {
                intensifierMultiplier = intensifiers.get(word);
                continue;
            }
            
            // Calculate word score
            double wordScore = 0.0;
            if (positiveWords.containsKey(word)) {
                wordScore = positiveWords.get(word);
            } else if (negativeWords.containsKey(word)) {
                wordScore = negativeWords.get(word);
            }
            
            if (wordScore != 0.0) {
                // Apply negation and intensifier
                if (negationActive) {
                    wordScore = -wordScore;
                    negationActive = false; // Reset after one word
                }
                wordScore *= intensifierMultiplier;
                intensifierMultiplier = 1.0; // Reset intensifier
                
                totalScore += wordScore;
                wordCount++;
            }
        }
        
        // Normalize by word count
        if (wordCount > 0) {
            return totalScore / Math.sqrt(wordCount); // Use sqrt for better normalization
        } else {
            return 0.0;
        }
    }
    
    /**
     * Add data point to time-series storage
     */
    private void addTimeSeriesData(String metric, double value, long timestamp) {
        List<Double> data = timeSeriesData.get(metric);
        List<Long> timestamps = timeSeriesTimestamps.get(metric);
        
        if (data != null && timestamps != null) {
            data.add(value);
            timestamps.add(timestamp);
            
            // Keep only last 1000 data points to prevent memory issues
            if (data.size() > 1000) {
                data.remove(0);
                timestamps.remove(0);
            }
        }
    }
    
    /**
     * Forecast next value in time-series
     */
    public double forecastTimeSeries(String metric) {
        try {
            List<Double> data = timeSeriesData.get(metric);
            if (data == null || data.size() < 10) {
                logger.warn("Insufficient data for {} forecasting", metric);
                return 0.0;
            }
            
            // Prepare last 10 data points as input
            int inputSize = Math.min(10, data.size());
            double[] input = new double[inputSize];
            for (int i = 0; i < inputSize; i++) {
                input[i] = data.get(data.size() - inputSize + i);
            }
            
            // Normalize input
            double mean = Arrays.stream(input).average().orElse(0.0);
            double std = Math.sqrt(Arrays.stream(input).map(x -> Math.pow(x - mean, 2)).average().orElse(1.0));
            
            for (int i = 0; i < input.length; i++) {
                input[i] = (input[i] - mean) / (std + 1e-8);
            }
            
            // Create input array for model
            INDArray modelInput = Nd4j.create(input).reshape(1, inputSize);
            
            // Get forecast
            INDArray output = timeSeriesForecaster.output(modelInput);
            double forecast = output.getDouble(0);
            
            // Denormalize
            forecast = forecast * (std + 1e-8) + mean;
            
            logger.debug("Time-series forecast for {}: {}", metric, forecast);
            return forecast;
            
        } catch (Exception e) {
            logger.error("Error in time-series forecasting: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Detect anomalies in time-series data
     */
    public boolean detectTimeSeriesAnomaly(String metric, double currentValue) {
        try {
            List<Double> data = timeSeriesData.get(metric);
            if (data == null || data.size() < 20) {
                return false; // Not enough data for anomaly detection
            }
            
            // Calculate rolling statistics (last 20 points)
            int windowSize = Math.min(20, data.size());
            List<Double> window = data.subList(data.size() - windowSize, data.size());
            
            double mean = window.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double std = Math.sqrt(window.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(1.0));
            
            // Z-score based anomaly detection
            double zScore = Math.abs((currentValue - mean) / (std + 1e-8));
            
            boolean isAnomaly = zScore > 3.0; // 3-sigma rule
            
            if (isAnomaly) {
                logger.warn("Time-series anomaly detected for {}: value={}, z-score={}", 
                           metric, currentValue, zScore);
            }
            
            return isAnomaly;
            
        } catch (Exception e) {
            logger.error("Error in time-series anomaly detection: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get time-series statistics
     */
    public Map<String, Object> getTimeSeriesStats(String metric) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Double> data = timeSeriesData.get(metric);
        if (data == null || data.isEmpty()) {
            stats.put("count", 0);
            stats.put("mean", 0.0);
            stats.put("std", 0.0);
            stats.put("min", 0.0);
            stats.put("max", 0.0);
            return stats;
        }
        
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double std = Math.sqrt(data.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0.0));
        
        stats.put("count", data.size());
        stats.put("mean", mean);
        stats.put("std", std);
        stats.put("min", data.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
        stats.put("max", data.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
        
        return stats;
    }
    
    /**
     * Analyze YouTube trending video data
     */
    public Map<String, Object> analyzeYouTubeVideo(YouTubeTrendingVideo video) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            // Calculate engagement rate
            long totalEngagement = (video.getLikeCount() != null ? video.getLikeCount() : 0) +
                                 (video.getCommentCount() != null ? video.getCommentCount() : 0);
            
            double engagementRate = 0.0;
            if (video.getViewCount() != null && video.getViewCount() > 0) {
                engagementRate = (double) totalEngagement / video.getViewCount();
            }
            
            // Analyze title sentiment
            double titleSentiment = calculateEnhancedSentimentScore(
                cleanText(video.getTitle().toLowerCase()));
            
            // Analyze description sentiment
            double descriptionSentiment = 0.0;
            if (video.getDescription() != null && !video.getDescription().trim().isEmpty()) {
                descriptionSentiment = calculateEnhancedSentimentScore(
                    cleanText(video.getDescription().toLowerCase()));
            }
            
            // Calculate virality score
            double viralityScore = calculateYouTubeViralityScore(video);
            
            // Determine trending potential
            boolean isHighlyTrending = viralityScore > 0.8 && engagementRate > 0.05;
            
            // Store metrics for time-series analysis
            if (video.getViewCount() != null) {
                addTimeSeriesData("youtube_views", video.getViewCount().doubleValue(), 
                                System.currentTimeMillis());
            }
            if (video.getLikeCount() != null) {
                addTimeSeriesData("youtube_likes", video.getLikeCount().doubleValue(), 
                                System.currentTimeMillis());
            }
            
            analysis.put("engagementRate", engagementRate);
            analysis.put("titleSentiment", titleSentiment);
            analysis.put("descriptionSentiment", descriptionSentiment);
            analysis.put("viralityScore", viralityScore);
            analysis.put("isHighlyTrending", isHighlyTrending);
            analysis.put("totalEngagement", totalEngagement);
            
            logger.debug("YouTube video analysis completed: Title={}, Virality={}, Trending={}", 
                        video.getTitle(), viralityScore, isHighlyTrending);
            
        } catch (Exception e) {
            logger.error("Error analyzing YouTube video: {}", e.getMessage());
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }
    
    /**
     * Calculate YouTube virality score
     */
    private double calculateYouTubeViralityScore(YouTubeTrendingVideo video) {
        double score = 0.0;
        
        // View count contribution (logarithmic scaling)
        if (video.getViewCount() != null && video.getViewCount() > 0) {
            score += Math.log10(video.getViewCount()) * 0.4;
        }
        
        // Like ratio contribution
        if (video.getViewCount() != null && video.getLikeCount() != null && video.getViewCount() > 0) {
            double likeRatio = (double) video.getLikeCount() / video.getViewCount();
            score += likeRatio * 100 * 0.3; // Scale up for visibility
        }
        
        // Comment engagement contribution
        if (video.getViewCount() != null && video.getCommentCount() != null && video.getViewCount() > 0) {
            double commentRatio = (double) video.getCommentCount() / video.getViewCount();
            score += commentRatio * 1000 * 0.3; // Comments are highly engaging
        }
        
        // Normalize to 0-1 range
        return Math.min(1.0, score / 10.0);
    }
}
