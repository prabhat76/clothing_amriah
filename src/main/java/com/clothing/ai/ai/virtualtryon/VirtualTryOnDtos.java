package com.clothing.ai.ai.virtualtryon;

public class VirtualTryOnDtos {
    public record TryOnResponse(String id, String userImageUrl, String productImageUrl,
                                  String styleAdvice, String userHeight,
                                  double confidence, String method) {}

    public record FitAnalysisResponse(String userImageUrl, String fitCategory, Double bmi,
                                        String recommendation, String productName) {}
}
