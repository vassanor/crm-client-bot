package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.service.KeyboardFactory;
import com.example.crmtgbot.service.MasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class MasterMenuCommandHandler implements UpdateHandler {

    private final BotIO io;
    private final MasterService ms;
    private final I18n i18n;
    private final KeyboardFactory kf;
    @Value("${bot.username}")
    private String botUsername;

    @Override
    public boolean canHandle(Update u) {
        return u.hasMessage() && u.getMessage().hasText() && (u.getMessage().getText().equals("/master") || u.getMessage().getText().equals("master"));
    }

    @Override
    public void handle(Update u) {
        Message m = u.getMessage();
        Long chatId = m.getChatId();
        m.getFrom().getFirstName();
        String display = m.getFrom().getFirstName() + (m.getFrom().getLastName() == null ? "" : " " + m.getFrom().getLastName());
        Master master = ms.getOrCreateMaster(chatId, m.getFrom().getId(), m.getFrom().getUserName(), display.trim());
        io.send(chatId, i18n.t("master.menu"), kf.masterMenu(master.isAutoConfirm()));
        io.send(chatId, i18n.t("master.link.hint", ms.deepLink(botUsername, master)), null);
    }
}
