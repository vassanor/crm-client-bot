package com.example.crmtgbot.service;

import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import com.example.crmtgbot.util.Emoji;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class KeyboardFactory {

    // ---------- helpers ----------
    private InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        InlineKeyboardRow r = new InlineKeyboardRow();
        r.addAll(Arrays.asList(buttons));
        return r;
    }

    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(data)
                .build();
    }

    private InlineKeyboardRow backRow(String data) {
        return row(btn(Emoji.BACK + " Назад", data));
    }

    // ---------- publics ----------
    public InlineKeyboardMarkup services(List<ServiceItem> services, Long masterId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (ServiceItem s : services) {
            rows.add(row(
                    btn(Emoji.SERVICE + " " + s.getName(), "B:svc:" + masterId + ":" + s.getId())
            ));
        }
        rows.add(backRow("B:back:home"));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup slots(List<TimeSlot> slots, Long masterId, Long serviceId, String backTarget) {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (TimeSlot s : slots) {
            rows.add(row(
                    btn(Emoji.CLOCK + " " + s.getStartTime().format(tf),
                            "B:slot:" + masterId + ":" + serviceId + ":" + s.getId())
            ));
        }
        rows.add(backRow(backTarget));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup nameChoice(String tgName, String backTarget) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn("👤 " + tgName, "B:name:use")));
        rows.add(row(btn("✏️ Ввести другое", "B:name:input")));
        rows.add(backRow(backTarget));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup confirmBooking(Long masterId, Long serviceId, Long slotId, String backTarget) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(Emoji.OK + " Подтвердить", "B:confirm:" + masterId + ":" + serviceId + ":" + slotId)));
        rows.add(row(btn(Emoji.NO + " Отмена", "B:back:" + backTarget)));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup masterApproval(Long bookingId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(
                btn(Emoji.OK + " Принять", "A:ok:" + bookingId),
                btn(Emoji.NO + " Отклонить", "A:no:" + bookingId)
        ));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup masterMenu(boolean auto) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn(Emoji.LINK + " Ссылка на запись", "M:link")));
        rows.add(row(btn(Emoji.SETTINGS + " Автоподтверждение: " + (auto ? "ON" : "OFF"), "M:auto")));
        rows.add(row(btn(Emoji.SERVICE + " Услуги", "M:services")));
        rows.add(row(btn(Emoji.CALENDAR + " Расписание", "M:schedule")));
        return new InlineKeyboardMarkup(rows);
    }
}
