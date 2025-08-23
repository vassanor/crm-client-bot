package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.service.KeyboardFactory;
import com.example.crmtgbot.service.MasterService;
import com.example.crmtgbot.service.SessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartHandler implements UpdateHandler {

    private final I18n i18n;
    private final BotIO io;
    private final MasterService masterService;
    private final KeyboardFactory kf;
    private final SessionStore store;
    @Value("${bot.username}")
    private String botUsername;

    @Override
    public boolean canHandle(Update u) {
        return u.hasMessage() && u.getMessage().hasText() && u.getMessage().getText().startsWith("/start");
    }

    @Override
    public void handle(Update u) {
        Message msg = u.getMessage();
        Long chatId = msg.getChatId();
        String text = msg.getText();
        String payload = text.contains(" ") ? text.substring(text.indexOf(' ') + 1) : "";
        if (payload.startsWith("book_")) {
            long masterId = Long.parseLong(payload.substring(5));
            var st = store.ensure(chatId);
            st.setMasterId(masterId);
            st.setServiceId(null);
            st.setSlotId(null);
            List<ServiceItem> services = masterService.findById(masterId).getServices();
            io.send(chatId, i18n.t("booking.choose.service"), kf.services(services, masterId));
            return;
        }
        msg.getFrom().getFirstName();
        msg.getFrom().getFirstName();
        String display = (msg.getFrom().getFirstName() +
                (msg.getFrom().getLastName() == null ? "":" "+msg.getFrom().getLastName())).trim();
        Master m = masterService.getOrCreateMaster(chatId, msg.getFrom().getId().intValue(), msg.getFrom().getUserName(), display);

        if (!masterService.isProfileComplete(m)) {
            String newsUrl = "https://t.me/your_news_channel"; // TODO: вынести в конфиг
            io.send(chatId,
                    i18n.t("welcome.title") + "\n\n" + i18n.t("welcome.subtitle"),
                    kf.welcome(newsUrl)
            );
            io.send(chatId, i18n.t("welcome.tip"), null);
            return;
        }

        io.send(chatId, i18n.t("master.menu"), kf.masterMenu(m.isAutoConfirm()));
        io.send(chatId, i18n.t("master.link.hint", masterService.deepLink(botUsername, m)), null);
    }
}
