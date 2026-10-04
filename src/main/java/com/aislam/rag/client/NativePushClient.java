package com.aislam.rag.client;

import com.aislam.rag.config.PushProperties;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Expo Push olmadan doğrudan FCM (Android) + APNs (iOS).
 */
@Component
public class NativePushClient {

    private static final Logger log = LoggerFactory.getLogger(NativePushClient.class);
    private static final String FIREBASE_APP_NAME = "aislam-push";

    private final PushProperties properties;
    private final ObjectMapper objectMapper;
    private final boolean firebaseReady;
    private final ApnsClient apnsClient;

    public NativePushClient(PushProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.firebaseReady = initFirebase(properties);
        this.apnsClient = initApns(properties);
        if (properties.enabled()) {
            log.info(
                    "Native push ready firebase={} apns={}",
                    firebaseReady,
                    apnsClient != null
            );
        }
    }

    public record PushMessage(
            String token,
            String platform,
            String title,
            String body,
            Map<String, String> data,
            String androidChannelId
    ) {
    }

    public record PushTicket(String token, boolean ok, String errorCode, String message) {
    }

    public List<PushTicket> send(List<PushMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        List<PushTicket> tickets = new ArrayList<>(messages.size());
        List<PushMessage> androidBatch = new ArrayList<>();
        List<Integer> androidIndexes = new ArrayList<>();

        for (int i = 0; i < messages.size(); i += 1) {
            PushMessage message = messages.get(i);
            String platform = message.platform() == null ? "" : message.platform().toLowerCase();
            if ("ios".equals(platform)) {
                tickets.add(sendApns(message));
            } else if ("android".equals(platform)) {
                androidBatch.add(message);
                androidIndexes.add(i);
                tickets.add(null); // placeholder
            } else {
                tickets.add(new PushTicket(message.token(), false, "UNSUPPORTED_PLATFORM", platform));
            }
        }

        if (!androidBatch.isEmpty()) {
            List<PushTicket> androidTickets = sendFcmBatched(androidBatch);
            for (int j = 0; j < androidIndexes.size(); j += 1) {
                tickets.set(androidIndexes.get(j), androidTickets.get(j));
            }
        }

        return tickets;
    }

    private List<PushTicket> sendFcmBatched(List<PushMessage> messages) {
        if (!firebaseReady) {
            return messages.stream()
                    .map(m -> new PushTicket(m.token(), false, "FCM_NOT_CONFIGURED", "Firebase credentials missing"))
                    .toList();
        }

        List<PushTicket> tickets = new ArrayList<>(messages.size());
        int batchSize = properties.batchSize();
        for (int start = 0; start < messages.size(); start += batchSize) {
            int end = Math.min(start + batchSize, messages.size());
            tickets.addAll(sendFcmChunk(messages.subList(start, end)));
        }
        return tickets;
    }

    private List<PushTicket> sendFcmChunk(List<PushMessage> chunk) {
        List<Message> fcmMessages = new ArrayList<>(chunk.size());
        for (PushMessage message : chunk) {
            fcmMessages.add(toFcmMessage(message));
        }

        try {
            BatchResponse batch = FirebaseMessaging.getInstance(firebaseApp()).sendEach(fcmMessages);
            List<PushTicket> tickets = new ArrayList<>(chunk.size());
            List<SendResponse> responses = batch.getResponses();
            for (int i = 0; i < chunk.size(); i += 1) {
                PushMessage source = chunk.get(i);
                SendResponse response = responses.get(i);
                if (response.isSuccessful()) {
                    tickets.add(new PushTicket(source.token(), true, null, null));
                } else {
                    FirebaseMessagingException ex = response.getException();
                    String code = mapFcmError(ex);
                    String detail = ex != null ? ex.getMessage() : "send failed";
                    tickets.add(new PushTicket(source.token(), false, code, detail));
                }
            }
            return tickets;
        } catch (FirebaseMessagingException ex) {
            log.warn("FCM sendEach failed: {}", ex.getMessage());
            String code = mapFcmError(ex);
            return chunk.stream()
                    .map(m -> new PushTicket(m.token(), false, code, ex.getMessage()))
                    .toList();
        }
    }

    private Message toFcmMessage(PushMessage message) {
        Map<String, String> data = message.data() != null ? message.data() : Map.of();
        AndroidNotification.Builder androidNotification = AndroidNotification.builder()
                .setSound("default");
        if (message.androidChannelId() != null && !message.androidChannelId().isBlank()) {
            androidNotification.setChannelId(message.androidChannelId());
        }

        return Message.builder()
                .setToken(message.token())
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(androidNotification.build())
                        .build())
                // Android FCM token; iOS ayrı APNs yolundan gider.
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder().setSound("default").build())
                        .build())
                .build();
    }

    private PushTicket sendApns(PushMessage message) {
        if (apnsClient == null) {
            return new PushTicket(message.token(), false, "APNS_NOT_CONFIGURED", "APNs credentials missing");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> aps = new HashMap<>();
            Map<String, Object> alert = new HashMap<>();
            alert.put("title", message.title());
            alert.put("body", message.body());
            aps.put("alert", alert);
            aps.put("sound", "default");
            payload.put("aps", aps);
            if (message.data() != null) {
                payload.putAll(message.data());
            }

            String payloadJson = objectMapper.writeValueAsString(payload);
            String token = TokenUtil.sanitizeTokenString(message.token());
            SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                    token,
                    properties.apnsBundleId(),
                    payloadJson,
                    Instant.now().plusSeconds(3600)
            );

            PushNotificationResponse<SimpleApnsPushNotification> response =
                    apnsClient.sendNotification(notification).get(20, TimeUnit.SECONDS);

            if (response.isAccepted()) {
                return new PushTicket(message.token(), true, null, null);
            }
            String reason = response.getRejectionReason().orElse("rejected");
            return new PushTicket(message.token(), false, reason, reason);
        } catch (Exception ex) {
            log.warn("APNs send failed: {}", ex.getMessage());
            return new PushTicket(message.token(), false, "APNS_ERROR", ex.getMessage());
        }
    }

    private static String mapFcmError(FirebaseMessagingException ex) {
        if (ex == null) {
            return "FCM_ERROR";
        }
        MessagingErrorCode code = ex.getMessagingErrorCode();
        return code != null ? code.name() : "FCM_ERROR";
    }

    private boolean initFirebase(PushProperties properties) {
        if (!properties.firebaseConfigured()) {
            return false;
        }
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(properties.firebaseCredentialsJson().getBytes(StandardCharsets.UTF_8))
            );
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            if (FirebaseApp.getApps().stream().noneMatch(app -> FIREBASE_APP_NAME.equals(app.getName()))) {
                FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
            }
            return true;
        } catch (Exception ex) {
            log.error("Firebase init failed: {}", ex.getMessage());
            return false;
        }
    }

    private FirebaseApp firebaseApp() {
        return FirebaseApp.getInstance(FIREBASE_APP_NAME);
    }

    private ApnsClient initApns(PushProperties properties) {
        if (!properties.apnsConfigured()) {
            return null;
        }
        try {
            String host = properties.apnsProduction()
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
            return new ApnsClientBuilder()
                    .setApnsServer(host)
                    .setSigningKey(com.eatthepath.pushy.apns.auth.ApnsSigningKey.loadFromInputStream(
                            new ByteArrayInputStream(normalizePem(properties.apnsKeyPem()).getBytes(StandardCharsets.UTF_8)),
                            properties.apnsTeamId(),
                            properties.apnsKeyId()
                    ))
                    .build();
        } catch (Exception ex) {
            log.error("APNs init failed: {}", ex.getMessage());
            return null;
        }
    }

    private static String normalizePem(String pem) {
        // Env'e tek satır yazıldıysa \\n → gerçek satır sonu
        return pem.replace("\\n", "\n").trim();
    }

    @PreDestroy
    void shutdown() {
        if (apnsClient != null) {
            try {
                apnsClient.close().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // shutdown best-effort
            }
        }
    }
}
