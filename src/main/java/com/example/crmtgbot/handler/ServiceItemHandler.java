package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.example.crmtgbot.service.SessionStore.ServiceEditField;
import static com.example.crmtgbot.service.SessionStore.ServiceMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceItemHandler implements UpdateHandler {

    public enum Step { NAME, DESCRIPTION, ADDRESS, PRICE, DURATION, DONE }

    private final ServiceItemService serviceItemService;
    private final MasterService masterService;
    private final SessionStore sessions;
    private final KeyboardFactory kf;
    private final BotIO io;
    private final I18n i18n;

    @Override
    public boolean canHandle(Update u) {
        if (u.hasCallbackQuery()) {
            String d = u.getCallbackQuery().getData();
            return d != null && d.startsWith("S:");
        }
        if (u.hasMessage() && u.getMessage().hasText()) {
            return sessions.getServiceSession(u.getMessage().getChatId()) != null;
        }
        return false;
    }

    @Override
    public void handle(Update u) {
        if (u.hasCallbackQuery()) {
            CallbackQuery cq = u.getCallbackQuery();
            Long chatId = cq.getMessage().getChatId();
            String data = cq.getData();
            Master m = masterService.findByChatId(chatId);

            // -------- CREATE FLOW --------
            if (data.equals("S:add")) {
                // 🔧 важно: гасим режим профиля, чтобы он не перехватывал текст
                sessions.getProfile(chatId).ifPresent(p -> p.setActive(false));

                sessions.startCreateService(chatId);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("service.enter.name"),
                        kf.backTo("M:services"));
                return;
            }


            if (data.startsWith("S:view:")) {
                Long id = Long.parseLong(data.split(":")[2]);
                ServiceItem s = serviceItemService.get(id);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        serviceCard(s),
                        kf.serviceView(id));
                return;
            }

            if (data.startsWith("S:delete:")) {
                Long id = Long.parseLong(data.split(":")[2]);
                serviceItemService.delete(id);
                var list = serviceItemService.listForMaster(m.getId());
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("service.deleted"),
                        kf.servicesMenu(list));
                return;
            }

            if (data.equals("S:addr:profile")) {
                // в CREATE: подставить адрес профиля и перейти к PRICE
                var ss = sessions.getServiceSession(chatId);
                if (ss != null && ss.getMode() == ServiceMode.CREATE) {
                    ServiceItem draft = ss.getItem();
                    draft.setAddress(m.getAddress());
                    sessions.setServiceStep(chatId, Step.PRICE, draft);
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("service.enter.price"),
                            kf.backTo("M:services"));
                }
                return;
            }

            // -------- EDIT FLOW --------
            if (data.startsWith("S:edit:")) {
                String[] p = data.split(":");
                // S:edit:{id} -> открыть меню редактирования
                if (p.length == 3) {
                    Long id = Long.parseLong(p[2]);
                    sessions.startEditService(chatId, id);
                    ServiceItem s = serviceItemService.get(id);
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            serviceCard(s),
                            kf.serviceEditMenu(id));
                    return;
                }
                // S:edit:field:{id} -> начать ввод поля
                if (p.length == 4) {
                    Long id = Long.parseLong(p[3]);
                    sessions.startEditService(chatId, id); // гарантируем сессию
                    switch (p[2]) {
                        case "name" -> {
                            sessions.setServiceEditField(chatId, ServiceEditField.NAME);
                            io.edit(chatId, cq.getMessage().getMessageId(),
                                    "✏️ Введите новое название услуги",
                                    kf.serviceEditMenu(id));
                        }
                        case "desc" -> {
                            sessions.setServiceEditField(chatId, ServiceEditField.DESCRIPTION);
                            io.edit(chatId, cq.getMessage().getMessageId(),
                                    "✏️ Введите новое описание услуги",
                                    kf.serviceEditMenu(id));
                        }
                        case "addr" -> {
                            sessions.setServiceEditField(chatId, ServiceEditField.ADDRESS);
                            io.edit(chatId, cq.getMessage().getMessageId(),
                                    i18n.t("service.enter.address"),
                                    kf.addressEditChoice(id)); // «взять из профиля» / назад
                        }
                        case "price" -> {
                            sessions.setServiceEditField(chatId, ServiceEditField.PRICE);
                            io.edit(chatId, cq.getMessage().getMessageId(),
                                    "💰 Введите новую стоимость (число)",
                                    kf.serviceEditMenu(id));
                        }
                        case "dur" -> {
                            sessions.setServiceEditField(chatId, ServiceEditField.DURATION);
                            io.edit(chatId, cq.getMessage().getMessageId(),
                                    "⏱ Введите новую длительность в минутах (кратно 15)",
                                    kf.serviceEditMenu(id));
                        }
                    }
                    return;
                }
            }

            if (data.startsWith("S:addr:profile:")) {
                // в EDIT: подставить адрес профиля и сохранить
                Long id = Long.parseLong(data.split(":")[3]);
                ServiceItem s = serviceItemService.get(id);
                s.setAddress(m.getAddress());
                serviceItemService.save(s);
                sessions.clearServiceSession(chatId);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("service.updated"),
                        kf.serviceView(id));
                return;
            }

            return;
        }

        // ------- TEXT INPUT -------
        if (u.hasMessage() && u.getMessage().hasText()) {
            Long chatId = u.getMessage().getChatId();
            String text = u.getMessage().getText().trim();
            var ss = sessions.getServiceSession(chatId);

            if (ss == null) return;

            // EDIT mode: одно сообщение — сохраняем и показываем карточку
            if (ss.getMode() == ServiceMode.EDIT) {
                Long id = ss.getEditingItemId();
                ServiceItem s = serviceItemService.get(id);
                switch (ss.getEditField()) {
                    case NAME -> s.setName(text);
                    case DESCRIPTION -> s.setDescription(text);
                    case ADDRESS -> s.setAddress(text.equals("—") ? null : text);
                    case PRICE -> {
                        try { s.setPrice(Integer.parseInt(text)); }
                        catch (NumberFormatException e) { io.send(chatId, "⛔ Введите число!", kf.serviceEditMenu(id)); return; }
                    }
                    case DURATION -> {
                        try {
                            int dur = Integer.parseInt(text);
                            if (dur % 15 != 0) { io.send(chatId, "⛔ Длительность должна быть кратна 15 минутам", kf.serviceEditMenu(id)); return; }
                            s.setDuration(dur);
                        } catch (NumberFormatException e) { io.send(chatId, "⛔ Введите число!", kf.serviceEditMenu(id)); return; }
                    }
                    default -> {}
                }
                serviceItemService.save(s);
                sessions.clearServiceSession(chatId);
                io.send(chatId, i18n.t("service.updated"), kf.serviceView(id));
                return;
            }

            // CREATE mode: пошаговый мастер
            var draft = ss.getItem();
            switch (ss.getStep()) {
                case NAME -> {
                    draft.setName(text);
                    sessions.setServiceStep(chatId, Step.DESCRIPTION, draft);
                    io.send(chatId, i18n.t("service.enter.description"), kf.backTo("M:services"));
                }
                case DESCRIPTION -> {
                    draft.setDescription(text);
                    sessions.setServiceStep(chatId, Step.ADDRESS, draft);
                    io.send(chatId, i18n.t("service.enter.address"), kf.addressChoice("M:services"));
                }
                case ADDRESS -> {
                    draft.setAddress(text.equals("—") ? null : text);
                    sessions.setServiceStep(chatId, Step.PRICE, draft);
                    io.send(chatId, i18n.t("service.enter.price"), kf.backTo("M:services"));
                }
                case PRICE -> {
                    try {
                        draft.setPrice(Integer.parseInt(text));
                        sessions.setServiceStep(chatId, Step.DURATION, draft);
                        io.send(chatId, i18n.t("service.enter.duration"), kf.backTo("M:services"));
                    } catch (NumberFormatException e) {
                        io.send(chatId, "⛔ Введите число!", kf.backTo("M:services"));
                    }
                }
                case DURATION -> {
                    try {
                        int dur = Integer.parseInt(text);
                        if (dur % 15 != 0) {
                            io.send(chatId, "⛔ Длительность должна быть кратна 15 минутам", kf.backTo("M:services"));
                            return;
                        }
                        draft.setDuration(dur);
                        Master m = masterService.findByChatId(chatId);
                        draft.setMaster(m);
                        serviceItemService.save(draft);
                        sessions.clearServiceSession(chatId);
                        io.send(chatId, i18n.t("service.saved"),
                                kf.servicesMenu(serviceItemService.listForMaster(m.getId())));
                    } catch (NumberFormatException e) {
                        io.send(chatId, "⛔ Введите число!", kf.backTo("M:services"));
                    }
                }
                default -> {}
            }
        }
    }

    private String serviceCard(ServiceItem s) {
        return "🛠 " + nn(s.getName()) + "\n\n" +
                "📖 " + nn(s.getDescription()) + "\n" +
                "📍 " + nn(s.getAddress()) + "\n" +
                "💰 " + nn(s.getPrice()) + "\n" +
                "⏱ " + nn(s.getDuration()) + " мин";
    }

    private String nn(Object o) { return o == null ? "—" : o.toString(); }
}
