package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Booking;
import com.example.crmtgbot.repo.BookingRepository;
import com.example.crmtgbot.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
public class MasterApprovalHandler implements UpdateHandler {

    private final BookingRepository repo;
    private final BookingService svc;
    private final BotIO io;
    private final I18n i18n;

    @Override
    public boolean canHandle(Update u) {
        return u.hasCallbackQuery() && u.getCallbackQuery().getData() != null && u.getCallbackQuery().getData().startsWith("A:");
    }

    @Override
    public void handle(Update u) {
        CallbackQuery cq = u.getCallbackQuery();
        String[] p = cq.getData().split(":");
        if (p.length < 3) return;
        Long id = Long.parseLong(p[2]);
        Booking b = repo.findById(id).orElse(null);
        if (b == null) return;
        switch (p[1]) {
            case "ok" -> {
                svc.approve(b);
                io.edit(cq.getMessage().getChatId(), cq.getMessage().getMessageId(), i18n.t("master.approved"), null);
                io.send(b.getClientChatId(), i18n.t("booking.approved.client"), null);
            }
            case "no" -> {
                svc.reject(b);
                io.edit(cq.getMessage().getChatId(), cq.getMessage().getMessageId(), i18n.t("master.rejected"), null);
                io.send(b.getClientChatId(), i18n.t("booking.rejected.client"), null);
            }
        }
        io.answerCallback(cq.getId(), "OK", false);
    }
}
