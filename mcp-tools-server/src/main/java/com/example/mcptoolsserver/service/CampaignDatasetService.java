
package com.example.mcptoolsserver.service;

import org.springframework.core.io.ClassPathResource;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CampaignDatasetService {

    private static final String DATASET_PATH = "data/campaign_performance.csv";

    private final List<Map<String, Object>> rows = loadDataset();

    private List<Map<String, Object>> loadDataset() {

        List<Map<String, Object>> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(

                new ClassPathResource(DATASET_PATH).getInputStream(),

                StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {

                return records;
            }

            String[] columns = header.split(",");

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {

                    continue;
                }

                String[] values = line.split(",", -1);

                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 0; i < columns.length && i < values.length; i++) {
                    row.put(columns[i].trim(), values[i].trim());
                }

                records.add(row);
            }

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not load campaign dataset: " + exception.getMessage(),
                    exception);
        }

        return records;
    }

    private int asInt(Object value) {
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    private double asDouble(Object value) {
        return value == null ? 0.0 : Double.parseDouble(value.toString());
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public Map<String, Object> getSummary() {

        if (rows.isEmpty()) {

            return Map.of(
                    "message", "The campaign performance dataset is empty or unavailable.",
                    "totalCampaigns", 0
            );
        }

        long totalImpressions = 0;
        long totalClicks = 0;
        long totalConversions = 0;
        double totalSpend = 0.0;

        Map<String, long[]> perPlatform = new LinkedHashMap<>();

        Map<String, Double> spendPerPlatform = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            int impressions = asInt(row.get("impressions"));
            int clicks = asInt(row.get("clicks"));
            int conversions = asInt(row.get("conversions"));
            double spend = asDouble(row.get("spend_usd"));
            String platform = String.valueOf(row.get("platform"));

            totalImpressions += impressions;
            totalClicks += clicks;
            totalConversions += conversions;
            totalSpend += spend;

            long[] agg = perPlatform.computeIfAbsent(platform, p -> new long[3]);
            agg[0] += impressions;
            agg[1] += clicks;
            agg[2] += conversions;

            spendPerPlatform.merge(platform, spend, Double::sum);
        }

        List<Map<String, Object>> platformBreakdown = new ArrayList<>();
        for (var entry : perPlatform.entrySet()) {
            long[] agg = entry.getValue();

            double ctr = agg[0] == 0 ? 0 : (agg[1] * 100.0) / agg[0];

            double conversionRate = agg[1] == 0 ? 0 : (agg[2] * 100.0) / agg[1];

            platformBreakdown.add(Map.of(
                    "platform", entry.getKey(),
                    "impressions", agg[0],
                    "clicks", agg[1],
                    "conversions", agg[2],
                    "ctrPercent", round2(ctr),
                    "conversionRatePercent", round2(conversionRate),
                    "spendUsd", round2(spendPerPlatform.get(entry.getKey()))
            ));
        }

        double overallCtr = totalImpressions == 0 ? 0 : (totalClicks * 100.0) / totalImpressions;
        double overallConversionRate = totalClicks == 0 ? 0 : (totalConversions * 100.0) / totalClicks;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCampaigns", rows.size());
        result.put("totalImpressions", totalImpressions);
        result.put("totalClicks", totalClicks);
        result.put("totalConversions", totalConversions);
        result.put("totalSpendUsd", round2(totalSpend));
        result.put("overallCtrPercent", round2(overallCtr));
        result.put("overallConversionRatePercent", round2(overallConversionRate));
        result.put("platformBreakdown", platformBreakdown);
        return result;
    }

    public Map<String, Object> getPlatformStats(String platform) {

        List<Map<String, Object>> matched = rows.stream()
                .filter(row -> String.valueOf(row.get("platform"))
                        .equalsIgnoreCase(platform))
                .toList();

        if (matched.isEmpty()) {

            return Map.of(
                    "platform", platform,
                    "found", false,
                    "message", "No campaigns found for platform '" + platform
                            + "' in the dataset."
            );
        }

        long impressions = 0, clicks = 0, conversions = 0;
        double spend = 0.0;

        for (Map<String, Object> row : matched) {
            impressions += asInt(row.get("impressions"));
            clicks += asInt(row.get("clicks"));
            conversions += asInt(row.get("conversions"));
            spend += asDouble(row.get("spend_usd"));
        }

        double ctr = impressions == 0 ? 0 : (clicks * 100.0) / impressions;
        double conversionRate = clicks == 0 ? 0 : (conversions * 100.0) / clicks;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platform", platform);
        result.put("found", true);
        result.put("campaignCount", matched.size());
        result.put("impressions", impressions);
        result.put("clicks", clicks);
        result.put("conversions", conversions);
        result.put("spendUsd", round2(spend));
        result.put("ctrPercent", round2(ctr));
        result.put("conversionRatePercent", round2(conversionRate));
        return result;
    }

    public List<Map<String, Object>> getTopCampaigns(int limit) {

        int effectiveLimit = Math.min(Math.max(limit, 1), rows.size());

        return rows.stream()

                .sorted((a, b) -> Integer.compare(
                        asInt(b.get("conversions")), asInt(a.get("conversions"))))

                .limit(effectiveLimit)

                .map(row -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("campaignName", row.get("campaign_name"));
                    entry.put("platform", row.get("platform"));
                    entry.put("conversions", asInt(row.get("conversions")));
                    entry.put("clicks", asInt(row.get("clicks")));
                    entry.put("impressions", asInt(row.get("impressions")));
                    entry.put("date", row.get("date"));
                    return entry;
                })
                .toList();
    }
}
