package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.service.KeyboardFactory;
import com.example.crmtgbot.service.MasterService;
import com.example.crmtgbot.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(5) // обрабатываем раньше прочих
public class StartHandler implements UpdateHandler {

    private final BotIO io;
    private final I18n i18n;
    private final MasterService masterService;
    private final ServiceItemService serviceItemService;
    private final KeyboardFactory kf;

    @Value("${app.newsUrl:https://t.me/your_news_channel}")
    private String newsUrl;

    @Override
    public boolean canHandle(Update u) {
        return u.hasMessage()
                && u.getMessage().hasText()
                && u.getMessage().getText().trim().startsWith("/start");
    }

    @Override
    public void handle(Update u) {
        Message msg = u.getMessage();
        Long chatId = msg.getChatId();
        String text = msg.getText().trim();

        // /start без payload → меню мастера (если профиль полный) или welcome
        String[] parts = text.split("\\s+", 2);
        String payload = parts.length > 1 ? parts[1] : "";

        if (payload.isBlank()) {
            var maybeMaster = masterService.findByChatId(chatId);
            if (maybeMaster != null && masterService.isProfileComplete(maybeMaster)) {
                io.send(chatId,
                        i18n.t("master.menu", safe(maybeMaster.getDisplayName())),
                        kf.masterMenu(maybeMaster.isAutoConfirm()));
            } else {
                // welcome-экран для мастера без профиля
                io.send(chatId,
                        i18n.t("welcome.title") + "\n" + i18n.t("welcome.subtitle") + "\n\n" + i18n.t("welcome.tip"),
                        kf.welcome(newsUrl));
            }
            return;
        }

        // /start с payload
        // Ожидаем стартовую ссылку «m<ID>», чтобы открыть запись к конкретному мастеру
        if (payload.startsWith("m")) {
            try {
                long masterId = Long.parseLong(payload.substring(1));
                var maybe = masterService.findByIdOpt(masterId);
                if (maybe.isEmpty()) {
                    io.send(chatId, "Мастер не найден. Попросите у него обновить ссылку.", null);
                    return;
                }
                Master m = maybe.get();

                // ✅ Берём услуги отдельным запросом, без ленивой коллекции
                List<ServiceItem> services = serviceItemService.listForMaster(m.getId());

                if (services.isEmpty()) {
                    io.send(chatId, i18n.t("services.empty"), kf.backTo("B:back:home"));
                    return;
                }
                io.send(chatId, i18n.t("booking.choose.service"), kf.services(services, m.getId()));
                return;
            } catch (NumberFormatException e) {
                io.send(chatId, "Некорректная ссылка для записи. Попросите у мастера новую.", null);
                return;
            }
        }



        // Неподдерживаемый payload — покажем дружелюбный фолбэк
        io.send(chatId, "Эта стартовая ссылка не поддерживается. Попробуйте ещё раз.", null);
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
