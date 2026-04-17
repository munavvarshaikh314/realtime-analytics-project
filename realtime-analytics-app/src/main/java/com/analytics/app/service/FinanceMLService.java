package com.analytics.app.service;

import com.analytics.app.model.FinanceMarketTick;
import com.analytics.app.model.FinancePrediction;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
@ConditionalOnProperty(value = "analytics.finance.enabled", havingValue = "true")
public class FinanceMLService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceMLService.class);
    private static final String MODEL_VERSION = "finance-lstm-v1";

    private final Map<String, Deque<FinanceMarketTick>> priceBuffers = new ConcurrentHashMap<>();
    private final Map<String, MultiLayerNetwork> symbolModels = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastModelTraining = new ConcurrentHashMap<>();
    private final Map<String, Boolean> trainingInProgress = new ConcurrentHashMap<>();

    @Autowired
    private ExecutorService financeTrainingExecutor;

    @Value("${analytics.finance.buffer-size:500}")
    private int bufferSize;

    @Value("${analytics.finance.sequence-length:20}")
    private int sequenceLength;

    @Value("${analytics.finance.retrain-threshold:60}")
    private int retrainThreshold;

    @Value("${analytics.finance.anomaly-threshold:2.0}")
    private double anomalyThreshold;

    public FinancePrediction analyzeTick(FinanceMarketTick tick) {
        List<FinanceMarketTick> history = registerTick(tick);

        if (history.size() >= retrainThreshold && shouldTriggerTraining(tick.getAssetId())) {
            triggerRetraining(tick.getAssetId(), history);
        }

        double predictedPrice = history.size() > sequenceLength
                ? predictWithModel(tick.getAssetId(), history)
                : predictWithFallback(history);

        double predictedChangePercentage = tick.getCurrentPrice() > 0
                ? ((predictedPrice - tick.getCurrentPrice()) / tick.getCurrentPrice()) * 100.0
                : 0.0;

        double anomalyScore = calculateAnomalyScore(history);
        double confidence = calculateConfidence(history, tick.getAssetId(), anomalyScore);
        String trendSignal = determineTrendSignal(predictedChangePercentage, anomalyScore);
        String insight = buildInsight(tick, predictedPrice, predictedChangePercentage, anomalyScore, confidence);

        FinancePrediction prediction = new FinancePrediction();
        prediction.setAssetId(tick.getAssetId());
        prediction.setSymbol(tick.getSymbol());
        prediction.setProvider(tick.getProvider());
        prediction.setCurrentPrice(tick.getCurrentPrice());
        prediction.setPredictedPrice(predictedPrice);
        prediction.setPredictedChangePercentage(predictedChangePercentage);
        prediction.setPriceChangePercentage24h(tick.getPriceChangePercentage24h());
        prediction.setMarketCap(tick.getMarketCap());
        prediction.setTotalVolume(tick.getTotalVolume());
        prediction.setAnomalyScore(anomalyScore);
        prediction.setPredictionConfidence(confidence);
        prediction.setTrendSignal(trendSignal);
        prediction.setInsight(insight);
        prediction.setModelVersion(MODEL_VERSION);
        prediction.setModelTrainedAt(lastModelTraining.get(tick.getAssetId()));
        prediction.setEventTimestamp(tick.getEventTimestamp());
        prediction.setCreatedAt(LocalDateTime.now());

        return prediction;
    }

    public Map<String, Object> retrainAsync(String assetId) {
        Map<String, Object> response = new HashMap<>();

        if (assetId == null || assetId.isBlank()) {
            for (String trackedAsset : new ArrayList<>(priceBuffers.keySet())) {
                triggerRetraining(trackedAsset, getHistory(trackedAsset));
            }
            response.put("status", "accepted");
            response.put("message", "Retraining scheduled for all tracked finance assets");
            response.put("assetId", "ALL");
            response.put("timestamp", LocalDateTime.now());
            return response;
        }

        List<FinanceMarketTick> history = getHistory(assetId);
        if (history.size() < retrainThreshold) {
            response.put("status", "rejected");
            response.put("message", "Not enough buffered finance ticks to train model");
            response.put("assetId", assetId);
            response.put("bufferSize", history.size());
            response.put("timestamp", LocalDateTime.now());
            return response;
        }

        triggerRetraining(assetId, history);
        response.put("status", "accepted");
        response.put("message", "Finance retraining scheduled");
        response.put("assetId", assetId);
        response.put("bufferSize", history.size());
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    public Map<String, Object> getModelStatus(String assetId) {
        Map<String, Object> status = new HashMap<>();
        List<FinanceMarketTick> history = getHistory(assetId);
        status.put("assetId", assetId);
        status.put("bufferSize", history.size());
        status.put("modelAvailable", symbolModels.containsKey(assetId));
        status.put("trainingInProgress", Boolean.TRUE.equals(trainingInProgress.get(assetId)));
        status.put("lastTrainedAt", lastModelTraining.get(assetId));
        status.put("modelVersion", MODEL_VERSION);
        return status;
    }

    public List<FinanceMarketTick> getHistory(String assetId) {
        Deque<FinanceMarketTick> buffer = priceBuffers.getOrDefault(assetId, new ArrayDeque<>());
        return new ArrayList<>(buffer);
    }

    private List<FinanceMarketTick> registerTick(FinanceMarketTick tick) {
        Deque<FinanceMarketTick> buffer = priceBuffers.computeIfAbsent(tick.getAssetId(), key -> new ArrayDeque<>());
        synchronized (buffer) {
            buffer.addLast(tick);
            while (buffer.size() > bufferSize) {
                buffer.removeFirst();
            }
            return new ArrayList<>(buffer);
        }
    }

    private boolean shouldTriggerTraining(String assetId) {
        return !Boolean.TRUE.equals(trainingInProgress.get(assetId));
    }

    private void triggerRetraining(String assetId, List<FinanceMarketTick> history) {
        trainingInProgress.put(assetId, true);
        financeTrainingExecutor.submit(() -> {
            try {
                trainModel(assetId, history);
            } finally {
                trainingInProgress.put(assetId, false);
            }
        });
    }

    private void trainModel(String assetId, List<FinanceMarketTick> history) {
        try {
            if (history.size() <= sequenceLength + 1) {
                logger.warn("Skipping finance model training for {} because the history is too short", assetId);
                return;
            }

            List<FinanceMarketTick> orderedHistory = new ArrayList<>(history);
            orderedHistory.sort(Comparator.comparing(FinanceMarketTick::getEventTimestamp));

            MultiLayerNetwork model = symbolModels.computeIfAbsent(assetId, ignored -> createLstmModel());
            DataSet dataSet = createTrainingData(orderedHistory);
            for (int epoch = 0; epoch < 12; epoch++) {
                model.fit(dataSet);
            }

            lastModelTraining.put(assetId, LocalDateTime.now());
            logger.info("Finance LSTM model training completed for {}", assetId);
        } catch (Exception e) {
            logger.error("Finance LSTM training failed for {}: {}", assetId, e.getMessage());
        }
    }

    private MultiLayerNetwork createLstmModel() {
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(42)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.005))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new GravesLSTM.Builder()
                        .nIn(3)
                        .nOut(24)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new GravesLSTM.Builder()
                        .nIn(24)
                        .nOut(16)
                        .activation(Activation.TANH)
                        .build())
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(16)
                        .nOut(1)
                        .activation(Activation.IDENTITY)
                        .build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(configuration);
        model.init();
        return model;
    }

    private DataSet createTrainingData(List<FinanceMarketTick> history) {
        int exampleCount = history.size() - sequenceLength;
        INDArray features = Nd4j.zeros(exampleCount, 3, sequenceLength);
        INDArray labels = Nd4j.zeros(exampleCount, 1, sequenceLength);

        for (int exampleIndex = 0; exampleIndex < exampleCount; exampleIndex++) {
            List<FinanceMarketTick> window = history.subList(exampleIndex, exampleIndex + sequenceLength + 1);
            double basePrice = Math.max(window.get(0).getCurrentPrice(), 1.0e-9);
            double maxVolume = Math.max(findMaxVolume(window), 1.0);

            for (int timeStep = 0; timeStep < sequenceLength; timeStep++) {
                FinanceMarketTick currentTick = window.get(timeStep);
                FinanceMarketTick nextTick = window.get(timeStep + 1);

                features.putScalar(new int[]{exampleIndex, 0, timeStep}, normalizePrice(currentTick.getCurrentPrice(), basePrice));
                features.putScalar(new int[]{exampleIndex, 1, timeStep}, normalizeVolume(currentTick.getTotalVolume(), maxVolume));
                features.putScalar(new int[]{exampleIndex, 2, timeStep}, normalizeChange(currentTick.getPriceChangePercentage24h()));

                labels.putScalar(new int[]{exampleIndex, 0, timeStep}, normalizePrice(nextTick.getCurrentPrice(), basePrice));
            }
        }

        return new DataSet(features, labels);
    }

    private double predictWithModel(String assetId, List<FinanceMarketTick> history) {
        MultiLayerNetwork model = symbolModels.get(assetId);
        if (model == null || !lastModelTraining.containsKey(assetId)) {
            return predictWithFallback(history);
        }

        try {
            List<FinanceMarketTick> orderedHistory = new ArrayList<>(history);
            orderedHistory.sort(Comparator.comparing(FinanceMarketTick::getEventTimestamp));
            List<FinanceMarketTick> window = orderedHistory.subList(orderedHistory.size() - sequenceLength, orderedHistory.size());

            double basePrice = Math.max(window.get(0).getCurrentPrice(), 1.0e-9);
            double maxVolume = Math.max(findMaxVolume(window), 1.0);
            INDArray features = Nd4j.zeros(1, 3, sequenceLength);

            for (int timeStep = 0; timeStep < sequenceLength; timeStep++) {
                FinanceMarketTick tick = window.get(timeStep);
                features.putScalar(new int[]{0, 0, timeStep}, normalizePrice(tick.getCurrentPrice(), basePrice));
                features.putScalar(new int[]{0, 1, timeStep}, normalizeVolume(tick.getTotalVolume(), maxVolume));
                features.putScalar(new int[]{0, 2, timeStep}, normalizeChange(tick.getPriceChangePercentage24h()));
            }

            INDArray output = model.output(features);
            double normalizedPrediction = output.getDouble(0, 0, sequenceLength - 1);
            return denormalizePrice(normalizedPrediction, basePrice);
        } catch (Exception e) {
            logger.warn("Falling back to heuristic finance prediction for {}: {}", assetId, e.getMessage());
            return predictWithFallback(history);
        }
    }

    private double predictWithFallback(List<FinanceMarketTick> history) {
        if (history.isEmpty()) {
            return 0.0;
        }

        if (history.size() < 3) {
            return history.get(history.size() - 1).getCurrentPrice();
        }

        double latest = history.get(history.size() - 1).getCurrentPrice();
        double shortAverage = history.stream()
                .skip(Math.max(0, history.size() - 5))
                .mapToDouble(FinanceMarketTick::getCurrentPrice)
                .average()
                .orElse(latest);

        double longAverage = history.stream()
                .skip(Math.max(0, history.size() - 15))
                .mapToDouble(FinanceMarketTick::getCurrentPrice)
                .average()
                .orElse(shortAverage);

        double momentum = shortAverage - longAverage;
        return Math.max(0.0, latest + momentum);
    }

    private double calculateAnomalyScore(List<FinanceMarketTick> history) {
        if (history.size() < 6) {
            return 0.0;
        }

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            double previousPrice = history.get(i - 1).getCurrentPrice();
            double currentPrice = history.get(i).getCurrentPrice();
            if (previousPrice > 0.0) {
                returns.add((currentPrice - previousPrice) / previousPrice);
            }
        }

        if (returns.size() < 2) {
            return 0.0;
        }

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = 0.0;
        for (Double value : returns) {
            variance += Math.pow(value - mean, 2);
        }
        variance = variance / (returns.size() - 1);
        double standardDeviation = Math.max(Math.sqrt(variance), 1.0e-6);
        double latestReturn = returns.get(returns.size() - 1);

        return Math.abs(latestReturn - mean) / standardDeviation;
    }

    private double calculateConfidence(List<FinanceMarketTick> history, String assetId, double anomalyScore) {
        double baseConfidence = lastModelTraining.containsKey(assetId) ? 0.78 : 0.55;
        double volatilityPenalty = Math.min(0.25, calculateVolatility(history) * 4.0);
        double anomalyPenalty = anomalyScore > anomalyThreshold ? 0.18 : 0.05;
        double confidence = baseConfidence - volatilityPenalty - anomalyPenalty;
        return Math.max(0.05, Math.min(0.98, confidence));
    }

    private double calculateVolatility(List<FinanceMarketTick> history) {
        if (history.size() < 5) {
            return 0.0;
        }

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            double previousPrice = history.get(i - 1).getCurrentPrice();
            double currentPrice = history.get(i).getCurrentPrice();
            if (previousPrice > 0.0) {
                returns.add((currentPrice - previousPrice) / previousPrice);
            }
        }

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = 0.0;
        for (Double value : returns) {
            variance += Math.pow(value - mean, 2);
        }
        return returns.size() > 1 ? Math.sqrt(variance / (returns.size() - 1)) : 0.0;
    }

    private String determineTrendSignal(double predictedChangePercentage, double anomalyScore) {
        if (anomalyScore > anomalyThreshold) {
            return "VOLATILE";
        }
        if (predictedChangePercentage > 1.0) {
            return "BULLISH";
        }
        if (predictedChangePercentage < -1.0) {
            return "BEARISH";
        }
        return "NEUTRAL";
    }

    private String buildInsight(
            FinanceMarketTick tick,
            double predictedPrice,
            double predictedChangePercentage,
            double anomalyScore,
            double confidence
    ) {
        StringBuilder insight = new StringBuilder();
        insight.append(tick.getSymbol().toUpperCase()).append(" is trading at ")
                .append(String.format("%.4f", tick.getCurrentPrice())).append(". ");

        if (predictedChangePercentage > 0.8) {
            insight.append("The short-term model expects upside continuation. ");
        } else if (predictedChangePercentage < -0.8) {
            insight.append("The short-term model expects downside pressure. ");
        } else {
            insight.append("The short-term model expects sideways consolidation. ");
        }

        if (anomalyScore > anomalyThreshold) {
            insight.append("Recent price behaviour is outside the normal volatility band. ");
        }

        insight.append("Projected next price is ").append(String.format("%.4f", predictedPrice))
                .append(" with confidence ").append(String.format("%.0f%%", confidence * 100.0)).append(".");

        return insight.toString();
    }

    private double findMaxVolume(List<FinanceMarketTick> history) {
        double maxVolume = 0.0;
        for (FinanceMarketTick tick : history) {
            if (tick.getTotalVolume() != null) {
                maxVolume = Math.max(maxVolume, tick.getTotalVolume());
            }
        }
        return maxVolume;
    }

    private double normalizePrice(Double currentPrice, double basePrice) {
        if (currentPrice == null || basePrice <= 0.0) {
            return 0.0;
        }
        return (currentPrice / basePrice) - 1.0;
    }

    private double denormalizePrice(double normalizedPrice, double basePrice) {
        return basePrice * (1.0 + normalizedPrice);
    }

    private double normalizeVolume(Double totalVolume, double maxVolume) {
        if (totalVolume == null || maxVolume <= 0.0) {
            return 0.0;
        }
        return totalVolume / maxVolume;
    }

    private double normalizeChange(Double priceChangePercentage24h) {
        return priceChangePercentage24h == null ? 0.0 : priceChangePercentage24h / 100.0;
    }

    @PreDestroy
    public void shutdown() {
        for (MultiLayerNetwork model : symbolModels.values()) {
            model.close();
        }
    }
}
