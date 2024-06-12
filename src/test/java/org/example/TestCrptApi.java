package org.example;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class TestCrptApi {

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
        CrptApi.Document document = createDocument();
        String signature = "dummySignature";
        long start = System.currentTimeMillis();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        for (int i = 1; i <= 25; i++) {
            forkJoinPool.execute(() -> {
                try {
                    api.createDocument(document, signature);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        forkJoinPool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.println("time: " + (System.currentTimeMillis() - start));
        api.shutdown();
    }

    public static CrptApi.Document createDocument() {
        CrptApi.Document document = new CrptApi.Document();
        document.description = new CrptApi.Document.Description();
        document.description.participantInn = "1234567890";
        document.docId = "doc123";
        document.docStatus = "NEW";
        document.docType = CrptApi.Document.DocType.LP_INTRODUCE_GOODS;
        document.importRequest = true;
        document.ownerInn = "1234567890";
        document.participantInn = "1234567890";
        document.producerInn = "1234567890";
        document.productionDate = LocalDate.of(2023, 1, 1);
        document.productionType = "TYPE_A";
        CrptApi.Document.Product product = new CrptApi.Document.Product();
        product.certificateDocument = "cert_doc";
        product.certificateDocumentDate = LocalDate.of(2023, 1, 1);
        product.certificateDocumentNumber = "cert123";
        product.ownerInn = "1234567890";
        product.producerInn = "1234567890";
        product.productionDate = LocalDate.of(2023, 1, 1);
        product.tnvedCode = "tnved123";
        product.uitCode = "uit123";
        product.uituCode = "uitu123";
        document.products = List.of(product);
        document.regDate = LocalDate.of(2023, 1, 1);
        document.regNumber = "reg123";
        return document;
    }
}
