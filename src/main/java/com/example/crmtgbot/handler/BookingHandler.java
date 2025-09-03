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
import java.util.List;

import static com.example.crmtgbot.handler.MasterProfileHandler.getString;


@Slf4j
@Component
@RequiredArgsConstructor
public class BookingHandler implements UpdateHandler {

    private final SessionStore store;
    private final MasterService masterService;
    private final ScheduleService scheduleService;
    private final BookingService bookingService;
    private final ServiceItemService serviceItemService;
    private final KeyboardFactory kf;
    private final BotIO io;
    private final I18n i18n;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public boolean canHandle(Update u) {
        if (u.hasCallbackQuery()) {
            var d = u.getCallbackQuery().getData();
            return d != null && (d.startsWith("B:"));
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
                ServiceItem s = serviceItemService.getForMaster(sid, mid);

                // список дней на 14 вперёд
                List<LocalDate> days = java.util.stream.IntStream.rangeClosed(0, 13)
                        .mapToObj(i -> LocalDate.now().plusDays(i))
                        .toList();

                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("booking.choose.day", s.getName()),
                        kf.days(mid, sid, days, d -> scheduleService.available(m, s, d).size(), "home"));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            if (data.startsWith("B:day:")) {
                var p = data.split(":");
                long mid = Long.parseLong(p[2]);
                long sid = Long.parseLong(p[3]);
                LocalDate day = LocalDate.parse(p[4]);

                st.setMasterId(mid);
                st.setServiceId(sid);

                Master m = masterService.findById(mid);
                ServiceItem s = serviceItemService.getForMaster(sid, mid);

                var slots = scheduleService.available(m, s, day);
                var df = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("booking.choose.time", s.getName(), df.format(day)),
                        kf.slots(slots, mid, sid, "B:svc:" + mid + ":" + sid)); // назад к дням той же услуги
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

                String uname = cq.getFrom().getUserName();
                String display = (uname != null && !uname.isBlank())
                        ? "@" + uname
                        : buildName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());

                st.setTempName(display);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("booking.confirm", st.getTempName()),
                        kf.confirmBooking(st.getMasterId(), st.getServiceId(), st.getSlotId(), "home"));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            if (data.equals("B:name:use")) {
                String tgName = buildName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                st.setTempName(tgName);
                st.setNeedNameInput(false);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("booking.confirm", st.getTempName()),
                        kf.confirmBooking(st.getMasterId(), st.getServiceId(), st.getSlotId(), "home"));
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

                Booking b = bookingService.create(
                        m, slot, sid,
                        chatId,
                        cq.getFrom().getId().intValue(),
                        cq.getFrom().getUserName(),
                        display
                );

                var tf = DateTimeFormatter.ofPattern("dd.MM HH:mm");
                String when = b.getSlot().getStartTime().format(tf);
                String addr = (b.getService().getAddress() != null && !b.getService().getAddress().isBlank())
                        ? b.getService().getAddress()
                        : (m.getAddress() == null ? "—" : m.getAddress());

                if (m.isAutoConfirm()) {
                    // клиенту — сразу c деталями
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("booking.done.auto", b.getService().getName(), when, addr), null);

                    // мастеру — уведомление (без кнопок)
                    if (m.getChatId() != null) {
                        io.send(m.getChatId(),
                                i18n.t("approval.request", display, b.getService().getName(), when) + "\n" +
                                        i18n.t("approval.accepted.master"),
                                null);
                    }

                    // (опционально) отметь статус как подтверждённый
                    bookingService.approve(b.getId());
                } else {
                    // мастеру — запрос на подтверждение
                    if (m.getChatId() != null) {
                        io.send(m.getChatId(),
                                i18n.t("approval.request", display, b.getService().getName(), when),
                                kf.masterApproval(b.getId()));
                    }
                    // клиенту — «ждите», но уже с деталями
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("booking.wait.approval", b.getService().getName(), when, addr), null);
                }


                io.answerCallback(cq.getId(), "OK", false);
                store.clear(chatId);
                return;
            }

            if (data.startsWith("B:back:")) {
                io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("home"), null);
                io.answerCallback(cq.getId(), "OK", false);
            }
        } else if (u.hasMessage() && u.getMessage().hasText()) {
            var m = u.getMessage();
            Long chatId = m.getChatId();
            var st = store.ensure(chatId);
            if (st.isNeedNameInput()) {
                st.setTempName(m.getText().trim());
                st.setNeedNameInput(false);
                io.send(chatId, i18n.t("booking.confirm", st.getTempName()),
                        kf.confirmBooking(st.getMasterId(), st.getServiceId(), st.getSlotId(), "home"));
            }
        }
    }

    private String buildName(String f, String l, String u) {
        return getString(f, l, u);
    }
}
