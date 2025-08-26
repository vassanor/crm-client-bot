package com.example.crmtgbot.service;

import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.example.crmtgbot.i18n.I18n;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeyboardFactory {

    private final I18n i18n;

    private InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        InlineKeyboardRow r = new InlineKeyboardRow();
        r.addAll(Arrays.asList(buttons));
        return r;
    }

    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }

    private InlineKeyboardRow backRowLabel(String labelKey, String data) {
        return row(btn(i18n.t(labelKey), data));
    }

    // ---------- WELCOME ----------
    public InlineKeyboardMarkup welcome(String newsUrl) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("welcome.create"), "P:start")));
        rows.add(row(InlineKeyboardButton.builder()
                .text(i18n.t("welcome.news"))
                .url(newsUrl)
                .build()));
        rows.add(row(btn(i18n.t("welcome.later"), "M:menu")));
        return new InlineKeyboardMarkup(rows);
    }

    // ---------- BOOKING (client) ----------
    public InlineKeyboardMarkup services(List<ServiceItem> services, Long masterId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (ServiceItem s : services) {
            rows.add(row(btn(i18n.t("booking.button.service", s.getName()),
                    "B:svc:" + masterId + ":" + s.getId())));
        }
        rows.add(backRowLabel("booking.button.back", "B:back:home"));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup slots(List<TimeSlot> slots, Long masterId, Long serviceId, String backTarget) {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (TimeSlot s : slots) {
            String label = i18n.t("booking.button.slot", s.getStartTime().format(tf));
            rows.add(row(btn(label, "B:slot:" + masterId + ":" + serviceId + ":" + s.getId())));
        }
        rows.add(backRowLabel("booking.button.back", backTarget));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup confirmBooking(Long masterId, Long serviceId, Long slotId, String backTarget) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("booking.button.confirm"), "B:confirm:" + masterId + ":" + serviceId + ":" + slotId)));
        rows.add(row(btn(i18n.t("booking.button.cancel"), "B:back:" + backTarget)));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup bookingNameChoice(String tgName, String backTarget) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(InlineKeyboardButton.builder()
                .text(i18n.t("booking.button.use.tgname", tgName))
                .callbackData("B:name:use")
                .build()));
        rows.add(backRowLabel("booking.button.back", "B:back:" + backTarget));
        return new InlineKeyboardMarkup(rows);
    }

    // ---------- MASTER APPROVAL ----------
    public InlineKeyboardMarkup masterApproval(Long bookingId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(
                btn(i18n.t("approval.button.accept"), "A:ok:" + bookingId),
                btn(i18n.t("approval.button.reject"), "A:no:" + bookingId)
        ));
        return new InlineKeyboardMarkup(rows);
    }

    // ---------- MASTER MENU ----------
    public InlineKeyboardMarkup masterMenu(boolean auto) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("menu.button.profile"), "M:profile")));
        rows.add(row(btn(i18n.t("menu.button.services"), "M:services")));
        rows.add(row(btn(i18n.t("menu.button.schedule"), "M:schedule")));
        rows.add(row(btn(i18n.t("menu.button.link"), "M:link")));
        rows.add(row(btn(i18n.t("menu.button.auto", (auto ? "ON" : "OFF")), "M:auto")));
        return new InlineKeyboardMarkup(rows);
    }

    // ---------- PROFILE ----------
    public InlineKeyboardMarkup profileNameChoice(String tgName) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("profile.button.take.from.tg", tgName), "P:name:use")));
        rows.add(backRowLabel("profile.button.back", "M:menu"));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup backTo(String backData) {
        return new InlineKeyboardMarkup(List.of(backRowLabel("common.back", backData)));
    }

    public InlineKeyboardMarkup profilePreviewSave() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("profile.button.save"), "P:save")));
        rows.add(backRowLabel("profile.button.back", "P:back:about"));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup profileView() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("profile.button.edit.name"), "P:edit:name")));
        rows.add(row(btn(i18n.t("profile.button.edit.address"), "P:edit:address")));
        rows.add(row(btn(i18n.t("profile.button.edit.about"), "P:edit:about")));
        rows.add(backRowLabel("profile.button.back", "M:menu"));
        return new InlineKeyboardMarkup(rows);
    }

    // ---------- SERVICES (master CRUD) ----------
    public InlineKeyboardMarkup servicesMenu(List<ServiceItem> list) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        if (list.isEmpty()) {
            rows.add(row(btn(i18n.t("services.button.add"), "S:add")));
        } else {
            for (ServiceItem s : list) {
                rows.add(row(btn("🛠 " + s.getName(), "S:view:" + s.getId()))); // название услуги всегда уникально на кнопке
            }
            rows.add(row(btn(i18n.t("services.button.add"), "S:add")));
        }
        rows.add(backRowLabel("services.button.back", "M:menu"));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup serviceView(Long id) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("services.button.edit"), "S:edit:" + id)));
        rows.add(row(btn(i18n.t("services.button.delete"), "S:delete:" + id)));
        rows.add(backRowLabel("services.button.back", "M:services"));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup serviceEditMenu(Long id) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn("✏️ Название", "S:edit:name:" + id)));
        rows.add(row(btn("✏️ Описание", "S:edit:desc:" + id)));
        rows.add(row(btn("✏️ Адрес", "S:edit:addr:" + id)));
        rows.add(row(btn("✏️ Стоимость", "S:edit:price:" + id)));
        rows.add(row(btn("✏️ Длительность", "S:edit:dur:" + id)));
        rows.add(backRowLabel("services.button.back", "S:view:" + id));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup addressChoice(String backData) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("services.button.address.from.profile"), "S:addr:profile")));
        rows.add(backRowLabel("services.button.back", backData));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup addressEditChoice(Long id) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("services.button.address.from.profile"), "S:addr:profile:" + id)));
        rows.add(backRowLabel("services.button.back", "S:edit:" + id));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup scheduleDayMenu(LocalDate day) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("schedule.day.view"), "C:view:"+ day)));
        rows.add(row(btn(i18n.t("schedule.day.generate"), "C:gen:"+ day)));
        rows.add(row(btn(i18n.t("schedule.day.clear"), "C:clear:"+ day)));
        rows.add(row(btn(i18n.t("schedule.back"), "C:back:week")));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup scheduleChooseService(List<ServiceItem> services) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (ServiceItem s : services) {
            rows.add(row(btn("🛠 " + s.getName(), "C:svc:"+ s.getId())));
        }
        rows.add(row(btn(i18n.t("schedule.back"), "C:back:day")));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup scheduleTimePresets(String backData) {
        // Несколько популярных окон (можно дополнять)
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn("10:00 — 18:00", "C:preset:10:00-18:00")));
        rows.add(row(btn("11:00 — 20:00", "C:preset:11:00-20:00")));
        rows.add(row(btn(i18n.t("schedule.back"), backData)));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup scheduleOverlapConfirm(LocalDate day) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("schedule.overlap.replace"), "C:over:replace:"+ day)));
        rows.add(row(btn(i18n.t("schedule.overlap.append"), "C:over:append:"+ day)));
        rows.add(row(btn(i18n.t("schedule.back"), "C:back:day")));
        return new InlineKeyboardMarkup(rows);
    }

    // Кнопка bulk на экране недели (ДОБАВЬ В scheduleWeek)
    public InlineKeyboardMarkup scheduleWeek(LocalDate monday, Map<LocalDate, Long> counters) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM");
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(
                btn(i18n.t("schedule.week.prev"), "C:week:prev"),
                btn(i18n.t("schedule.week.next"), "C:week:next")
        ));
        for (int i=0; i<7; i++){
            LocalDate d = monday.plusDays(i);
            long cnt = counters.getOrDefault(d, 0L);
            String label = i18n.t("schedule.day.button",
                    d.format(df),
                    cnt == 0 ? i18n.t("schedule.day.empty") : String.valueOf(cnt));
            rows.add(row(btn(label, "C:day:"+ d)));
        }
        rows.add(row(btn(i18n.t("schedule.week.bulk"), "C:bulk:start"))); // <— вот эта новая кнопка
        rows.add(row(btn(i18n.t("schedule.back"), "M:menu")));
        return new InlineKeyboardMarkup(rows);
    }

    // Выбор количества недель
    public InlineKeyboardMarkup scheduleBulkWeeks() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("schedule.bulk.weeks.1"), "C:bulk:w:1")));
        rows.add(row(btn(i18n.t("schedule.bulk.weeks.2"), "C:bulk:w:2")));
        rows.add(row(btn(i18n.t("schedule.bulk.weeks.4"), "C:bulk:w:4")));
        rows.add(row(btn(i18n.t("schedule.back"), "C:back:week")));
        return new InlineKeyboardMarkup(rows);
    }

    // Пикер дней недели (тоггл). selected — множество выбранных дней.
    public InlineKeyboardMarkup scheduleWeekdayPicker(java.util.EnumSet<java.time.DayOfWeek> selected) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(
                btn(labelFor(selected, java.time.DayOfWeek.MONDAY,  "weekday.mon"), "C:bulk:wd:MON"),
                btn(labelFor(selected, java.time.DayOfWeek.TUESDAY, "weekday.tue"), "C:bulk:wd:TUE"),
                btn(labelFor(selected, java.time.DayOfWeek.WEDNESDAY,"weekday.wed"), "C:bulk:wd:WED")
        ));
        rows.add(row(
                btn(labelFor(selected, java.time.DayOfWeek.THURSDAY,"weekday.thu"), "C:bulk:wd:THU"),
                btn(labelFor(selected, java.time.DayOfWeek.FRIDAY,  "weekday.fri"), "C:bulk:wd:FRI"),
                btn(labelFor(selected, java.time.DayOfWeek.SATURDAY,"weekday.sat"), "C:bulk:wd:SAT")
        ));
        rows.add(row(
                btn(labelFor(selected, java.time.DayOfWeek.SUNDAY,  "weekday.sun"), "C:bulk:wd:SUN")
        ));
        rows.add(row(
                btn(i18n.t("schedule.bulk.weekdays.all"), "C:bulk:wd:ALL"),
                btn(i18n.t("schedule.bulk.weekdays.done"), "C:bulk:wd:DONE")
        ));
        rows.add(row(btn(i18n.t("schedule.back"), "C:back:week")));
        return new InlineKeyboardMarkup(rows);
    }

    private String labelFor(java.util.EnumSet<java.time.DayOfWeek> selected, java.time.DayOfWeek d, String i18nKey) {
        String name = i18n.t(i18nKey);
        return (selected.contains(d) ? "✅ " : "⬜ ") + name;
    }

    // Подтверждение режима пересечений (bulk)
    public InlineKeyboardMarkup scheduleBulkOverlap() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("schedule.bulk.replace"), "C:bulk:over:replace")));
        rows.add(row(btn(i18n.t("schedule.bulk.append"), "C:bulk:over:append")));
        rows.add(row(btn(i18n.t("schedule.back"), "C:back:week")));
        return new InlineKeyboardMarkup(rows);
    }

    // Финальное подтверждение bulk
    public InlineKeyboardMarkup scheduleBulkConfirm() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(i18n.t("schedule.bulk.confirm"), "C:bulk:confirm")));
        rows.add(row(btn(i18n.t("schedule.bulk.cancel"), "C:back:week")));
        return new InlineKeyboardMarkup(rows);
    }

}

