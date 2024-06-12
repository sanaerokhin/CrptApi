package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    public static String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        this.objectMapper.registerModule(module);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        long timeIntervalMillis = timeUnit.toMillis(1);
        this.scheduler.scheduleAtFixedRate(
                () -> semaphore.release(requestLimit - semaphore.availablePermits()),
                timeIntervalMillis,
                timeIntervalMillis,
                TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        String json = objectMapper.writeValueAsString(document);
        HttpRequest request = createRequest(json, signature);
        HttpResponse<String> response;
        synchronized (CrptApi.class) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        if (response.statusCode() != 200) {
            System.out.println("Failed to create document: " + response.body());
        }
    }

    private HttpRequest createRequest(String json, String signature) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class Document {
        @JsonProperty("description")
        public Description description;
        @JsonProperty("doc_id")
        public String docId;
        @JsonProperty("doc_status")
        public String docStatus;
        @JsonProperty("doc_type")
        public DocType docType;

        @JsonProperty("importRequest")
        public boolean importRequest;

        @JsonProperty("owner_inn")
        public String ownerInn;
        @JsonProperty("participant_inn")
        public String participantInn;
        @JsonProperty("producer_inn")
        public String producerInn;
        @JsonProperty("production_date")
        public LocalDate productionDate;
        @JsonProperty("production_type")
        public String productionType;
        @JsonProperty("products")
        public List<Product> products;
        @JsonProperty("reg_date")
        public LocalDate regDate;
        @JsonProperty("reg_number")
        public String regNumber;

        public static class Description {
            @JsonProperty("participantInn")
            public String participantInn;
        }

        public static class Product {
            @JsonProperty("certificate_document")
            public String certificateDocument;
            @JsonProperty("certificate_document_date")
            public LocalDate certificateDocumentDate;
            @JsonProperty("certificate_document_number")
            public String certificateDocumentNumber;
            @JsonProperty("owner_inn")
            public String ownerInn;
            @JsonProperty("producer_inn")
            public String producerInn;
            @JsonProperty("production_date")
            public LocalDate productionDate;
            @JsonProperty("tnved_code")
            public String tnvedCode;
            @JsonProperty("uit_code")
            public String uitCode;
            @JsonProperty("uitu_code")
            public String uituCode;
        }

        public enum DocType {
            LP_INTRODUCE_GOODS
        }
    }
}