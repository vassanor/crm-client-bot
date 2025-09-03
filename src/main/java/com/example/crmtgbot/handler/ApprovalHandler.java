package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Booking;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.service.BookingService;
import com.example.crmtgbot.service.MasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// ApprovalHandler.java
@Component
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(30)
public class ApprovalHandler implements UpdateHandler {

    private final BookingService bookingService;
    private final MasterService masterService;
    private final BotIO io;
    private final I18n i18n;

    private static final java.time.format.DateTimeFormatter TF =
            java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm");

    @Override
    public boolean canHandle(org.telegram.telegrambots.meta.api.objects.Update u) {
        if (!u.hasCallbackQuery()) return false;
        String d = u.getCallbackQuery().getData();
        return d != null && (d.startsWith("A:ok:") || d.startsWith("A:no:"));
    }

    @Override
    public void handle(org.telegram.telegrambots.meta.api.objects.Update u) {
        var cq = u.getCallbackQuery();
        Long chatId = cq.getMessage().getChatId();
        String data = cq.getData();

        Long bookingId = Long.parseLong(data.substring(data.lastIndexOf(':') + 1));

        // грузим бронирование
        Booking b = "A:ok:".equals(data.substring(0, 5))
                ? bookingService.approve(bookingId)
                : bookingService.reject(bookingId);

        // простая проверка «мастер ли жмёт»
        Master m = b.getMaster();
        boolean allowed = (m.getChatId() != null && m.getChatId().equals(chatId));
        if (!allowed) {
            io.answerCallback(cq.getId(), "Недостаточно прав", true);
            return;
        }

        String when = b.getSlot().getStartTime().format(TF);
        String addr = (b.getService().getAddress() != null && !b.getService().getAddress().isBlank())
                ? b.getService().getAddress()
                : (m.getAddress() == null ? "—" : m.getAddress());

        if (data.startsWith("A:ok:")) {
            // мастеру: «подтверждено»
            io.edit(chatId, cq.getMessage().getMessageId(),
                    i18n.t("approval.accepted.master"), null);

            // клиенту: «подтверждено» с деталями
            if (b.getClientChatId() != null) {
                io.send(b.getClientChatId(),
                        i18n.t("booking.approved.client",
                                b.getService().getName(), when, addr),
                        null);
            }
        } else {
            // мастеру: «отклонено»
            io.edit(chatId, cq.getMessage().getMessageId(),
                    i18n.t("approval.rejected.master"), null);

            // клиенту: «отклонено»
            if (b.getClientChatId() != null) {
                io.send(b.getClientChatId(),
                        i18n.t("booking.rejected.client"), null);
            }
        }

        io.answerCallback(cq.getId(), "OK", false);
    }
}
