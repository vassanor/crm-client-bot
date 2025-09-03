package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.service.KeyboardFactory;
import com.example.crmtgbot.service.MasterService;
import com.example.crmtgbot.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

@RequiredArgsConstructor
@Component
@Slf4j
public class MasterMenuHandler implements UpdateHandler {

    private final MasterService masterService;
    private final KeyboardFactory kf;
    private final BotIO io;
    private final I18n i18n;
    private final MasterProfileHandler profileHandler; // <--- проброс
    private final ServiceItemService serviceItemService;
    private final ScheduleHandler scheduleHandler;

    @Value("${bot.username:your_bot_username}")
    private String botUsername;

    @Override
    public boolean canHandle(Update u) {
        if (!u.hasCallbackQuery()) return false;
        String data = u.getCallbackQuery().getData();
        return data != null && data.startsWith("M:");
    }

    @Override
    public void handle(Update u) {
        CallbackQuery cq = u.getCallbackQuery();
        String data = cq.getData();

        // Пробросим "M:profile" и "M:menu" в MasterProfileHandler
        if ("M:profile".equals(data) || "M:menu".equals(data)) {
            profileHandler.handle(u);
            return;
        }

        Long chatId = cq.getMessage().getChatId();
        Master m = masterService.getOrCreateMaster(
                chatId,
                cq.getFrom().getId(),
                cq.getFrom().getUserName(),
                buildName(cq)
        );

        switch (data) {
            case "M:auto" -> {
                masterService.toggleAuto(m);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("master.menu"),
                        kf.masterMenu(m.isAutoConfirm()));
                io.answerCallback(cq.getId(), "OK", false);
            }
            case "M:link" -> {
                        buildName(cq);
                String url = masterService.deepLink(m);
                io.answerCallback(cq.getId(), i18n.t("ok"), false);
                io.send(chatId, i18n.t("master.link.hint", url), null);
            }
            case "M:services" -> {
                var list = serviceItemService.listForMaster(m.getId()); // <--- нужен бин serviceItemService
                String text = list.isEmpty()
                        ? i18n.t("services.empty")
                        : i18n.t("services.title");
                io.edit(chatId, cq.getMessage().getMessageId(), text, kf.servicesMenu(list));
                io.answerCallback(cq.getId(), "OK", false);
            }

            case "M:schedule" -> scheduleHandler.handle(u);
            default -> io.answerCallback(cq.getId(), "OK", false);
        }
    }

    private String buildName(CallbackQuery cq) {
        String f = cq.getFrom().getFirstName();
        String l = cq.getFrom().getLastName();
        String u = cq.getFrom().getUserName();
        return MasterProfileHandler.getString(f, l, u);
    }
}

