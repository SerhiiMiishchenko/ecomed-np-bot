package com.github.mishchuk7.ecomednpbot.v1.client;

import com.github.mishchuk7.ecomednpbot.v1.enums.CargoType;
import com.github.mishchuk7.ecomednpbot.v1.enums.TrackingStatusCode;
import com.github.mishchuk7.ecomednpbot.v1.exception.EcomedNpTelegramBotException;
import com.github.mishchuk7.ecomednpbot.v1.model.*;
import com.github.mishchuk7.ecomednpbot.v1.util.HttpRequestUtils;
import com.github.mishchuk7.ecomednpbot.v1.util.SearchRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class InternetDocumentManagerImpl implements InternetDocumentManager {


    private final DocumentManagerConfig documentManagerConfig;

    @Override
    public List<InternetDocument> getAllDocuments(SearchRequest searchRequest) throws IOException, InterruptedException {
        InternetDocumentResponse internetDocumentResponse = HttpRequestUtils.post(documentManagerConfig.getBaseUrl(), searchRequest, InternetDocumentResponse.class);
        logResponseError(internetDocumentResponse);
        return internetDocumentResponse.getData().stream()
                .map(InternetDocumentResponse.ResultData::result)
                .flatMap(Collection::stream)
                .toList();
    }

    @Override
    public double getTotalWeightOfParcelsAtBranch(List<InternetDocument> internetDocuments, String city, int branchNumber) {
        List<TrackingDocument> trackingDocuments = receiveParcelsArrivedAtBranch(internetDocuments, city, branchNumber);
        String numbers = trackingDocuments.stream().map(t -> t.getNumber() + " : " + t.getDocumentWeight()).collect(Collectors.joining("; "));
        log.info("List of arrived parcels: " + numbers);
        return trackingDocuments.stream()
                .mapToDouble(TrackingDocument::getDocumentWeight)
                .sum();
    }

    @Override
    public int getQuantityOfPallet(List<InternetDocument> internetDocuments, String city, int branchNumber) {
        List<TrackingDocument> trackingDocuments = receiveParcelsArrivedAtBranch(internetDocuments, city, branchNumber);
        return trackingDocuments.stream()
                .filter(document -> document.getCargoType().equalsIgnoreCase(CargoType.PALLET.getRef()))
                .mapToInt(TrackingDocument::getSeatsAmount)
                .sum();
    }

    @Override
    public int getTotalNumberOfSeats(List<InternetDocument> internetDocuments, String city, int branchNumber) {
        List<TrackingDocument> trackingDocuments = receiveParcelsArrivedAtBranch(internetDocuments, city, branchNumber);
        return trackingDocuments.stream()
                .mapToInt(TrackingDocument::getSeatsAmount)
                .sum();
    }

    private List<TrackingDocument> receiveParcelsArrivedAtBranch(List<InternetDocument> internetDocuments, String city, int branchNumber) {
        return internetDocuments.stream()
                .filter(document -> city.equals(document.getCityRecipientDescription())
                        && document.getRecipientAddressDescription().contains(String.valueOf(branchNumber)))
                .filter(document -> document.getTrackingStatusCode() == TrackingStatusCode.ARRIVED_AT_BRANCH.getId())
                .map(document -> new MethodProperties.Document(document.getNumber(), document.getPhoneSender()))
                .map(document -> SearchRequestUtils.createSearchRequestTrackingDoc(document, documentManagerConfig))
                .map(searchRequest -> {
                    try {
                        return new TrackingDocumentManagerImpl(documentManagerConfig).getAllDocuments(searchRequest);
                    } catch (IOException | InterruptedException e) {
                        log.warn("Interrupted", e);
                        Thread.currentThread().interrupt();
                        throw new EcomedNpTelegramBotException("SearchRequest create error", e);
                    }
                })
                .flatMap(Collection::stream)
                .toList();
    }

    private static void logResponseError(InternetDocumentResponse internetDocumentResponse) {
        if (!internetDocumentResponse.getErrors().isEmpty()) {
            log.warn(internetDocumentResponse.getErrors().get(0));
        }
        if (!internetDocumentResponse.getWarnings().isEmpty()) {
            log.warn(internetDocumentResponse.getWarnings().get(0));
        }
        if (!internetDocumentResponse.getErrorCodes().isEmpty()) {
            log.warn(internetDocumentResponse.getErrorCodes().get(0));
        }
    }
}
