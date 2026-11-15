package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ads-ssv")
public record AdsSsvProperties(
        /** true: bonus sadece AdMob SSV callback ile. */
        boolean enabled,
        /** Beklenen ad unit id (opsiyonel; boşsa kontrol yok). */
        String rewardedAdUnitId
) {
    public AdsSsvProperties {
        if (rewardedAdUnitId == null) {
            rewardedAdUnitId = "";
        }
    }
}
