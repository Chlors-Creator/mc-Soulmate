package com.mcsoulmate.client;

public record ApiConfiguration(String provider, String baseUrl, String apiKey, String model) {
    public static ApiConfiguration empty() {
        return new ApiConfiguration("", "", "", "");
    }

    public boolean isComplete() {
        return !this.provider.isBlank()
                && !this.baseUrl.isBlank()
                && !this.apiKey.isBlank()
                && !this.model.isBlank();
    }
}
