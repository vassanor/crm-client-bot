package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Booking;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.service.*;
import com.example.crmtgbot.service.SessionStore.BookingSessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.example.crmtgbot.handler.MasterProfileHandler.getString;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingHandler implements UpdateHandler {

    private final SessionStore store;
    private final MasterService masterService;
    private final ScheduleService scheduleService;
    private final BookingService bookingService;
    private final KeyboardFactory kf;
    private final BotIO io;
    private final I18n i18n;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public boolean canHandle(Update u) {
        if (u.hasCallbackQuery()) {
            var d = u.getCallbackQuery().getData();
            return d != null && (d.startsWith("B:") || d.startsWith("M:"));
        }
        if (u.hasMessage() && u.getMessage().hasText()) {
            Long chatId = u.getMessage().getChatId();
            return store.get(chatId).map(BookingSessionState::isNeedNameInput).orElse(false);
        }
        return false;
    }

    @Override
    public void handle(Update u) {
        if (u.hasCallbackQuery()) {
            var cq = u.getCallbackQuery();
            Long chatId = cq.getMessage().getChatId();
            String data = cq.getData();
            var st = store.ensure(chatId);
            if (data.startsWith("B:svc:")) {
                var p = data.split(":");
                long mid = Long.parseLong(p[2]);
                long sid = Long.parseLong(p[3]);
                st.setMasterId(mid);
                st.setServiceId(sid);
                Master m = masterService.findById(mid);
                ServiceItem s = m.getServices().stream().filter(it -> it.getId().equals(sid)).findFirst().orElseThrow();
                var day = LocalDate.now();
                var slots = scheduleService.available(m, s, day);
                io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("booking.choose.time", s.getName(), DF.format(day)), kf.slots(slots, mid, sid, "home"));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }
            if (data.startsWith("B:slot:")) {
                var p = data.split(":");
                long mid = Long.parseLong(p[2]);
                long sid = Long.parseLong(p[3]);
                long slot = Long.parseLong(p[4]);
                st.setMasterId(mid);
                st.setServiceId(sid);
                st.setSlotId(slot);
                String tgName = buildName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                st.setNeedNameInput(false);
                io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("booking.name.ask"), kf.nameChoice(tgName, "home"));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }
            if (data.equals("B:name:use")) {
                String tgName = buildName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                st.setTempName(tgName);
                io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("booking.confirm", st.getTempName()), kf.confirmBooking(st.getMasterId(), st.getServiceId(), st.getSlotId(), "home"));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }
            if (data.equals("B:name:input")) {
                st.setNeedNameInput(true);
                io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("booking.name.input"), null);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }
            if (data.startsWith("B:confirm:")) {
                var p = data.split(":");
                long mid = Long.parseLong(p[2]);
                long sid = Long.parseLong(p[3]);
                long slot = Long.parseLong(p[4]);
                String display = st.getTempName();
                Master m = masterService.findById(mid);
                Booking b = bookingService.create(m, slot, sid, chatId, cq.getFrom().getId().intValue(), cq.getFrom().getUserName(), display);
                if (m.isAutoConfirm())
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("booking.done.auto"), null);
                else {
                    if (m.getChatId() != null)
                        io.send(m.getChatId(), i18n.t("master.approve.request", display, b.getService().getName(), b.getSlot().getStartTime().toString()), new KeyboardFactory().masterApproval(b.getId()));
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("booking.wait.approval"), null);
                }
                io.answerCallback(cq.getId(), "OK", false);
                store.clear(chatId);
                return;
            }
            if (data.startsWith("B:back:")) {
                io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("home"), null);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }
            switch (data) {
                case "M:auto" -> {
                    Master m = masterService.getOrCreateMaster(chatId, cq.getFrom().getId().intValue(), cq.getFrom().getUserName(), buildName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName()));
                    masterService.toggleAuto(m);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("master.menu"), new KeyboardFactory().masterMenu(m.isAutoConfirm()));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "M:link" -> {
                    Master m = masterService.getOrCreateMaster(chatId, cq.getFrom().getId().intValue(), cq.getFrom().getUserName(), buildName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName()));
                    io.answerCallback(cq.getId(), "Ссылка отправлена", false);
                    io.send(chatId, i18n.t("master.link.hint", masterService.deepLink(System.getProperty("bot.username", "your_bot"), m)), null);
                    return;
                }
                case "M:services", "M:schedule" -> io.answerCallback(cq.getId(), "Скоро будет готово 🚧", false);
            }
        } else if (u.hasMessage() && u.getMessage().hasText()) {
            var m = u.getMessage();
            Long chatId = m.getChatId();
            var st = store.ensure(chatId);
            if (st.isNeedNameInput()) {
                st.setTempName(m.getText().trim());
                st.setNeedNameInput(false);
                io.send(chatId, i18n.t("booking.confirm", st.getTempName()), new KeyboardFactory().confirmBooking(st.getMasterId(), st.getServiceId(), st.getSlotId(), "home"));
            }
        }
    }

    private String buildName(String f, String l, String u) {
        return getString(f, l, u);
    }
}
