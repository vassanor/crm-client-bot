package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import com.example.crmtgbot.service.KeyboardFactory;
import com.example.crmtgbot.service.MasterService;
import com.example.crmtgbot.service.ScheduleService;
import com.example.crmtgbot.service.ServiceItemService;
import com.example.crmtgbot.service.SessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(25)
public class ScheduleHandler implements UpdateHandler {

    private final SessionStore sessions;
    private final MasterService masterService;
    private final ServiceItemService serviceItemService;
    private final ScheduleService scheduleService;
    private final KeyboardFactory kf;
    private final BotIO io;
    private final I18n i18n;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public boolean canHandle(Update u) {
        if (u.hasCallbackQuery()) {
            String d = u.getCallbackQuery().getData();
            return d != null && (d.startsWith("C:") || d.equals("M:schedule"));
        }
        if (u.hasMessage() && u.getMessage().hasText()) {
            return sessions.getCalendar(u.getMessage().getChatId())
                    .map(SessionStore.CalendarSession::isActive).orElse(false);
        }
        return false;
    }

    @Override
    public void handle(Update u) {
        if (u.hasCallbackQuery()) {
            CallbackQuery cq = u.getCallbackQuery();
            Long chatId = cq.getMessage().getChat().getId();
            String data = cq.getData();
            Master m = masterService.findByChatId(chatId);
            var cs = sessions.ensureCalendar(chatId);

            // вход из меню расписания
            if ("M:schedule".equals(data)) {
                renderWeek(chatId, cq.getMessage().getMessageId(), cs, m);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // навигация недель
            if ("C:week:prev".equals(data)) {
                cs.setWeekStart(cs.getWeekStart().minusWeeks(1));
                renderWeek(chatId, cq.getMessage().getMessageId(), cs, m);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }
            if ("C:week:next".equals(data)) {
                cs.setWeekStart(cs.getWeekStart().plusWeeks(1));
                renderWeek(chatId, cq.getMessage().getMessageId(), cs, m);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            if ("C:back:week".equals(data)) {
                renderWeek(chatId, cq.getMessage().getMessageId(), cs, m);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // возврат к дню (в bulk уходим на неделю)
            if ("C:back:day".equals(data)) {
                if (Boolean.TRUE.equals(cs.isBulk())) {
                    renderWeek(chatId, cq.getMessage().getMessageId(), cs, m);
                } else if (cs.getTargetDay() != null) {
                    LocalDate day = cs.getTargetDay();
                    long cnt = scheduleService.countDaySlots(m.getId(), day);
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("schedule.day.menu", DF.format(day), String.valueOf(cnt)),
                            kf.scheduleDayMenu(day, cnt));
                } else {
                    renderWeek(chatId, cq.getMessage().getMessageId(), cs, m);
                }
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // выбор дня
            if (data.startsWith("C:day:")) {
                LocalDate day = LocalDate.parse(data.substring("C:day:".length()));
                cs.setTargetDay(day);
                long cnt = scheduleService.countDaySlots(m.getId(), day);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("schedule.day.menu", DF.format(day), String.valueOf(cnt)),
                        kf.scheduleDayMenu(day, cnt));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // просмотр слотов дня
            if (data.startsWith("C:view:")) {
                LocalDate day = LocalDate.parse(data.substring("C:view:".length()));
                List<TimeSlot> slots = scheduleService.listDaySlots(m.getId(), day);
                StringBuilder sb = new StringBuilder(i18n.t("schedule.view.slots.title", DF.format(day))).append("\n");
                for (TimeSlot s : slots) {
                    sb.append(i18n.t("schedule.view.slots.line", s.getStartTime().format(TF))).append("\n");
                }
                if (slots.isEmpty()) sb.append(i18n.t("schedule.day.empty"));
                io.edit(chatId, cq.getMessage().getMessageId(), sb.toString(), kf.scheduleDayMenu(day, slots.size()));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // взять выходной (очистить день)
            if (data.startsWith("C:dayoff:")) {
                LocalDate day = LocalDate.parse(data.substring("C:dayoff:".length()));
                scheduleService.clearDay(m.getId(), day);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("schedule.day.dayoff.done"),
                        kf.scheduleDayMenu(day, 0));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // генерация слотов для одного дня: выбор услуги
            if (data.startsWith("C:gen:")) {
                LocalDate day = LocalDate.parse(data.substring("C:gen:".length()));
                cs.setTargetDay(day);
                var services = serviceItemService.listForMaster(m.getId());
                if (services.isEmpty()) {
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            "Сначала создайте услугу в меню «🛠 Услуги».", kf.scheduleDayMenu(day, 0));
                } else {
                    cs.setActive(true);
                    cs.setStep(SessionStore.CalendarSession.Step.PICK_SERVICE);
                    cs.setBulk(false);
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("schedule.choose.service"),
                            kf.scheduleChooseService(services));
                }
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // ===== БЫСТРАЯ НАСТРОЙКА: старт с недели =====
            if ("C:setup:start".equals(data)) {
                cs.setBulk(true);
                cs.setWeeks(null);
                cs.setWeekdays(java.util.EnumSet.noneOf(DayOfWeek.class));
                cs.setServiceId(null);
                cs.setStartStr(null);
                cs.setEndStr(null);
                cs.setBulkReplace(null);
                cs.setStep(SessionStore.CalendarSession.Step.PICK_SERVICE);

                var services = serviceItemService.listForMaster(m.getId());
                if (services.isEmpty()) {
                    io.answerCallback(cq.getId(), "Сначала создайте услугу в «🛠 Услуги»", true);
                } else {
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("schedule.choose.service"),
                            kf.scheduleChooseService(services));
                    io.answerCallback(cq.getId(), "OK", false);
                }
                return;
            }

            // общий выбор услуги (и day, и bulk)
            if (data.startsWith("C:svc:")) {
                Long svcId = Long.parseLong(data.substring("C:svc:".length()));
                cs.setServiceId(svcId);

                if (Boolean.TRUE.equals(cs.isBulk())) {
                    cs.setStep(SessionStore.CalendarSession.Step.BULK_WEEKS);
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("schedule.bulk.weeks.ask"),
                            kf.scheduleBulkWeeks());
                } else {
                    cs.setStep(SessionStore.CalendarSession.Step.ASK_START);
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("schedule.ask.start"),
                            kf.scheduleTimePresets("C:back:day"));
                }
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // ===== BULK: выбор количества недель =====
            if (data.startsWith("C:bulk:w:")) {
                int w = Integer.parseInt(data.substring("C:bulk:w:".length()));
                cs.setWeeks(w);
                cs.setStep(SessionStore.CalendarSession.Step.BULK_WEEKDAYS);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("schedule.bulk.weekdays.ask"),
                        kf.scheduleWeekdayPicker(cs.getWeekdays()));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // ===== BULK: выбор рабочих дней (тоггл/все/готово) =====
            if (data.startsWith("C:bulk:wd:")) {
                String tail = data.substring("C:bulk:wd:".length());
                switch (tail) {
                    case "ALL" -> cs.setWeekdays(java.util.EnumSet.of(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
                    case "DONE" -> {
                        if (cs.getWeekdays().isEmpty()) {
                            io.answerCallback(cq.getId(), "Выберите хотя бы один день", true);
                            return;
                        }
                        cs.setStep(SessionStore.CalendarSession.Step.ASK_START);
                        io.edit(chatId, cq.getMessage().getMessageId(),
                                i18n.t("schedule.bulk.ask.start"),
                                kf.scheduleTimePresets("C:back:week"));
                        io.answerCallback(cq.getId(), "OK", false);
                        return;
                    }
                    default -> {
                        DayOfWeek d = parseDow(tail);
                        if (cs.getWeekdays().contains(d)) cs.getWeekdays().remove(d);
                        else cs.getWeekdays().add(d);
                    }
                }
                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("schedule.bulk.weekdays.ask"),
                        kf.scheduleWeekdayPicker(cs.getWeekdays()));
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // пресеты времени (и для day, и для bulk)
            if (data.startsWith("C:preset:")) {
                String range = data.substring("C:preset:".length()); // "10:00-18:00"
                String[] parts = range.split("-");
                cs.setStartStr(parts[0]);
                cs.setEndStr(parts[1]);

                if (Boolean.TRUE.equals(cs.isBulk())) {
                    boolean anyExisting = hasAnyExisting(m.getId(), cs.getWeekStart(), cs.getWeeks(), cs.getWeekdays());
                    if (anyExisting) {
                        cs.setStep(SessionStore.CalendarSession.Step.BULK_OVERLAP);
                        io.edit(chatId, cq.getMessage().getMessageId(),
                                i18n.t("schedule.bulk.overlap"),
                                kf.scheduleBulkOverlap());
                    } else {
                        cs.setBulkReplace(false); // по умолчанию добавление
                        cs.setStep(SessionStore.CalendarSession.Step.BULK_CONFIRM);
                        io.edit(chatId, cq.getMessage().getMessageId(),
                                bulkSummaryText(cs), kf.scheduleBulkConfirm());
                    }
                } else {
                    long existing = scheduleService.countDaySlots(m.getId(), cs.getTargetDay());
                    if (existing > 0) {
                        cs.setStep(SessionStore.CalendarSession.Step.OVERLAP);
                        io.edit(chatId, cq.getMessage().getMessageId(),
                                i18n.t("schedule.overlap.confirm"),
                                kf.scheduleOverlapConfirm(cs.getTargetDay()));
                    } else {
                        doGenerate(chatId, cq.getMessage().getMessageId(), cs, m, false);
                    }
                }
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // day-overlap подтверждение
            if (data.startsWith("C:over:replace:") || data.startsWith("C:over:append:")) {
                boolean replace = data.startsWith("C:over:replace:");
                doGenerate(chatId, cq.getMessage().getMessageId(), cs, m, replace);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // ===== BULK: выбор режима пересечений =====
            if ("C:bulk:over:replace".equals(data) || "C:bulk:over:append".equals(data)) {
                cs.setBulkReplace("C:bulk:over:replace".equals(data));
                cs.setStep(SessionStore.CalendarSession.Step.BULK_CONFIRM);
                io.edit(chatId, cq.getMessage().getMessageId(),
                        bulkSummaryText(cs),
                        kf.scheduleBulkConfirm());
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            // ===== BULK: подтверждение генерации =====
            if ("C:bulk:confirm".equals(data)) {
                ServiceItem svc = serviceItemService.get(cs.getServiceId());
                boolean replace = Boolean.TRUE.equals(cs.getBulkReplace());

                int created = scheduleService.bulkGenerateSlots(
                        m, svc, cs.getWeekStart(), cs.getWeeks(),
                        cs.getWeekdays(),
                        LocalTime.parse(cs.getStartStr(), TF),
                        LocalTime.parse(cs.getEndStr(), TF),
                        replace
                );

                io.edit(chatId, cq.getMessage().getMessageId(),
                        i18n.t("schedule.bulk.generated", String.valueOf(created)),
                        kf.scheduleWeek(cs.getWeekStart(),
                                buildCounters(m, cs.getWeekStart()),
                                scheduleService.hasAnySlots(m.getId())));
                // сброс bulk-состояния
                cs.setBulk(false);
                cs.setWeeks(null);
                cs.setWeekdays(java.util.EnumSet.noneOf(DayOfWeek.class));
                cs.setServiceId(null);
                cs.setStartStr(null);
                cs.setEndStr(null);
                cs.setBulkReplace(null);
                cs.setStep(SessionStore.CalendarSession.Step.NONE);
                io.answerCallback(cq.getId(), "OK", false);
                return;
            }

            return;
        }

        // -------- TEXT INPUT (start/end times) --------
        if (u.hasMessage() && u.getMessage().hasText()) {
            Long chatId = u.getMessage().getChatId();
            String text = u.getMessage().getText().trim();
            var cs = sessions.ensureCalendar(chatId);
            Master m = masterService.findByChatId(chatId);

            switch (cs.getStep()) {
                case ASK_START -> {
                    if (!isTime(text)) {
                        io.send(chatId, i18n.t("schedule.invalid.time"),
                                kf.scheduleTimePresets(Boolean.TRUE.equals(cs.isBulk()) ? "C:back:week" : "C:back:day"));
                        return;
                    }
                    cs.setStartStr(text);
                    cs.setStep(SessionStore.CalendarSession.Step.ASK_END);
                    io.send(chatId,
                            Boolean.TRUE.equals(cs.isBulk()) ? i18n.t("schedule.bulk.ask.end") : i18n.t("schedule.ask.end"),
                            kf.scheduleTimePresets(Boolean.TRUE.equals(cs.isBulk()) ? "C:back:week" : "C:back:day"));
                }
                case ASK_END -> {
                    if (!isTime(text)) {
                        io.send(chatId, i18n.t("schedule.invalid.time"),
                                kf.scheduleTimePresets(Boolean.TRUE.equals(cs.isBulk()) ? "C:back:week" : "C:back:day"));
                        return;
                    }
                    cs.setEndStr(text);

                    if (Boolean.TRUE.equals(cs.isBulk())) {
                        boolean anyExisting = hasAnyExisting(m.getId(), cs.getWeekStart(), cs.getWeeks(), cs.getWeekdays());
                        if (anyExisting) {
                            cs.setStep(SessionStore.CalendarSession.Step.BULK_OVERLAP);
                            io.send(chatId, i18n.t("schedule.bulk.overlap"), kf.scheduleBulkOverlap());
                        } else {
                            cs.setBulkReplace(false); // по умолчанию добавление
                            cs.setStep(SessionStore.CalendarSession.Step.BULK_CONFIRM);
                            io.send(chatId, bulkSummaryText(cs), kf.scheduleBulkConfirm());
                        }
                    } else {
                        long existing = scheduleService.countDaySlots(m.getId(), cs.getTargetDay());
                        if (existing > 0) {
                            cs.setStep(SessionStore.CalendarSession.Step.OVERLAP);
                            io.send(chatId, i18n.t("schedule.overlap.confirm"), kf.scheduleOverlapConfirm(cs.getTargetDay()));
                        } else {
                            doGenerate(chatId, null, cs, m, false);
                        }
                    }
                }
                default -> { /* ignore */ }
            }
        }
    }

    private void renderWeek(Long chatId, Integer msgId, SessionStore.CalendarSession cs, Master m) {
        LocalDate mon = cs.getWeekStart();
        LocalDate sun = mon.plusDays(6);
        boolean hasAny = scheduleService.hasAnySlots(m.getId());
        Map<LocalDate, Long> counters = new LinkedHashMap<>();
        if (hasAny) {
            for (int i = 0; i < 7; i++) {
                LocalDate d = mon.plusDays(i);
                counters.put(d, scheduleService.countDaySlots(m.getId(), d));
            }
        }
        String title = hasAny
                ? i18n.t("schedule.title", mon.format(DF), sun.format(DF))
                : i18n.t("schedule.week.empty");

        if (msgId == null)
            io.send(chatId, title, kf.scheduleWeek(mon, counters, hasAny));
        else
            io.edit(chatId, msgId, title, kf.scheduleWeek(mon, counters, hasAny));
    }

    private boolean isTime(String s) {
        try {
            LocalTime.parse(s, TF);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void doGenerate(Long chatId, Integer msgId, SessionStore.CalendarSession cs,
                            Master m, boolean replace) {
        LocalDate day = cs.getTargetDay();
        LocalTime start = LocalTime.parse(cs.getStartStr(), TF);
        LocalTime end = LocalTime.parse(cs.getEndStr(), TF);
        ServiceItem svc = serviceItemService.get(cs.getServiceId());
        int created = scheduleService.generateSlots(m, svc, day, start, end, replace);

        String txt = i18n.t("schedule.generated", String.valueOf(created));
        long cnt = scheduleService.countDaySlots(m.getId(), day);
        if (msgId == null)
            io.send(chatId, txt + "\n\n" + i18n.t("schedule.day.menu", DF.format(day), String.valueOf(cnt)),
                    kf.scheduleDayMenu(day, cnt));
        else
            io.edit(chatId, msgId, txt + "\n\n" + i18n.t("schedule.day.menu", DF.format(day), String.valueOf(cnt)),
                    kf.scheduleDayMenu(day, cnt));

        // сброс состояния day-flow
        cs.setStep(SessionStore.CalendarSession.Step.NONE);
        cs.setServiceId(null);
        cs.setStartStr(null);
        cs.setEndStr(null);
        cs.setActive(false);
    }

    private boolean hasAnyExisting(Long masterId, LocalDate weekStart, Integer weeks,
                                   java.util.EnumSet<DayOfWeek> days) {
        if (weekStart == null || weeks == null || days == null || days.isEmpty()) return false;
        for (int w = 0; w < weeks; w++) {
            LocalDate base = weekStart.plusWeeks(w);
            for (DayOfWeek d : days) {
                LocalDate day = base.with(d);
                if (scheduleService.countDaySlots(masterId, day) > 0) return true;
            }
        }
        return false;
    }

    private String bulkSummaryText(SessionStore.CalendarSession cs) {
        String days = cs.getWeekdays().stream()
                .sorted()
                .map(d -> switch (d) {
                    case MONDAY -> i18n.t("weekday.mon");
                    case TUESDAY -> i18n.t("weekday.tue");
                    case WEDNESDAY -> i18n.t("weekday.wed");
                    case THURSDAY -> i18n.t("weekday.thu");
                    case FRIDAY -> i18n.t("weekday.fri");
                    case SATURDAY -> i18n.t("weekday.sat");
                    case SUNDAY -> i18n.t("weekday.sun");
                })
                .reduce((a, b) -> a + ", " + b).orElse("-");
        String mode = Boolean.TRUE.equals(cs.getBulkReplace()) ? i18n.t("mode.replace") : i18n.t("mode.append");
        return i18n.t("schedule.bulk.summary",
                serviceItemService.get(cs.getServiceId()).getName(),
                String.valueOf(cs.getWeeks()),
                days,
                cs.getStartStr(), cs.getEndStr(),
                mode);
    }

    private Map<LocalDate, Long> buildCounters(Master m, LocalDate weekStart) {
        Map<LocalDate, Long> counters = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            counters.put(d, scheduleService.countDaySlots(m.getId(), d));
        }
        return counters;
    }

    private DayOfWeek parseDow(String code) {
        return switch (code) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Unknown weekday code: " + code);
        };
    }
}
