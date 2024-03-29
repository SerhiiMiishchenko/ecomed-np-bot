package com.github.mishchuk7.ecomednpbot.v1.handler;

import com.github.mishchuk7.ecomednpbot.v1.client.DocumentManagerConfig;
import com.github.mishchuk7.ecomednpbot.v1.client.InternetDocumentManager;
import com.github.mishchuk7.ecomednpbot.v1.client.InternetDocumentManagerImpl;
import com.github.mishchuk7.ecomednpbot.v1.model.InternetDocument;
import com.github.mishchuk7.ecomednpbot.v1.model.SearchRequest;
import com.github.mishchuk7.ecomednpbot.v1.model.UserRequest;
import com.github.mishchuk7.ecomednpbot.v1.service.TelegramService;
import com.github.mishchuk7.ecomednpbot.v1.util.SearchRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class WeightCommandHandler extends UserRequestHandler {

    private static final String WEIGHT = "/weight";
    private static final int BRANCH_NUMBER = 66;
    private static final String CITY = "Київ";


    @Value("${BASE_PHONE_NUMBER}")
    private String baseNumber;

    private final TelegramService telegramService;
    private final DocumentManagerConfig documentManagerConfig;


    @Override
    public boolean isApplicable(UserRequest request) {
        return isCommand(request.getUpdate(), WEIGHT);
    }

    @Override
    public void handle(UserRequest dispatchRequest) {
        String commonWeight = "Вага відправлень: ";
        String seatsAmount = "Кількість місць: ";
        String palletsQuantity = "Палети: ";
        double weight = 0d;
        int seats = 0;
        int pallets = 0;
        try {
            InternetDocumentManager internetDocumentManager = new InternetDocumentManagerImpl(documentManagerConfig);
            SearchRequest searchRequest = SearchRequestUtils.createSearchRequestInternetDoc(baseNumber, documentManagerConfig);
            List<InternetDocument> internetDocuments = internetDocumentManager.getAllDocuments(searchRequest);
            weight = internetDocumentManager.getTotalWeightOfParcelsAtBranch(internetDocuments, CITY, BRANCH_NUMBER);
            pallets = internetDocumentManager.getQuantityOfPallet(internetDocuments, CITY, BRANCH_NUMBER);
            seats = internetDocumentManager.getTotalNumberOfSeats(internetDocuments, CITY, BRANCH_NUMBER);
        } catch (IOException | InterruptedException e) {
            log.warn("Interrupted!", e);
            Thread.currentThread().interrupt();
        }
        String message = "%s<b>%.2f</b>\n%s<b>%d</b>\n%s<b>%d</b>";
        telegramService.sendMessage(dispatchRequest.getChatId(), String.format(message, commonWeight, weight, seatsAmount, seats, palletsQuantity, pallets));
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

}
