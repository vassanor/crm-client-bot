// src/main/java/com/example/crmtgbot/service/KeyboardFactory.java
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

    private InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        InlineKeyboardRow r = new InlineKeyboardRow();
        r.addAll(Arrays.asList(buttons));
        return r;
    }
    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }
    private InlineKeyboardRow backRow(String data) { return row(btn(Emoji.BACK + " Назад", data)); }

    // WELCOME
    public InlineKeyboardMarkup welcome(String newsUrl) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn("✨ Создать профиль", "P:start")));
        rows.add(row(InlineKeyboardButton.builder().text("📣 Наш новостной канал").url(newsUrl).build()));
        rows.add(row(btn("⏭️ Позже", "M:menu")));
        return new InlineKeyboardMarkup(rows);
    }

    // Клиентские клавиатуры (без изменений)
    public InlineKeyboardMarkup services(List<ServiceItem> services, Long masterId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (ServiceItem s : services) {
            rows.add(row(btn(Emoji.SERVICE + " " + s.getName(), "B:svc:" + masterId + ":" + s.getId())));
        }
        rows.add(backRow("B:back:home"));
        return new InlineKeyboardMarkup(rows);
    }
    public InlineKeyboardMarkup slots(List<TimeSlot> slots, Long masterId, Long serviceId, String backTarget) {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (TimeSlot s : slots) {
            rows.add(row(btn(Emoji.CLOCK + " " + s.getStartTime().format(tf),
                    "B:slot:" + masterId + ":" + serviceId + ":" + s.getId())));
        }
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
        rows.add(row(btn("👤 Профиль", "M:profile")));
        rows.add(row(btn(Emoji.SERVICE + " Услуги", "M:services")));
        rows.add(row(btn(Emoji.CALENDAR + " Расписание", "M:schedule")));
        rows.add(row(btn(Emoji.LINK + " Ссылка на запись", "M:link")));
        rows.add(row(btn(Emoji.SETTINGS + " Автоподтверждение: " + (auto ? "ON" : "OFF"), "M:auto")));
        return new InlineKeyboardMarkup(rows);
    }

    // Профиль: только «взять из TG» + «Назад»
    public InlineKeyboardMarkup profileNameChoice(String tgName){
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn("👤 " + tgName + " (взять из TG)", "P:name:use")));
        rows.add(backRow("M:menu"));
        return new InlineKeyboardMarkup(rows);
    }

    // Кнопки «Назад», пока ждём текст
    public InlineKeyboardMarkup backTo(String backData) {
        return new InlineKeyboardMarkup(List.of(backRow(backData)));
    }

    // Предпросмотр/сохранение
    public InlineKeyboardMarkup profilePreviewSave(){
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn("✅ Сохранить профиль", "P:save")));
        rows.add(backRow("P:back:about"));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup profileView() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(btn("✏️ Имя", "P:edit:name")));
        rows.add(row(btn("✏️ Адрес", "P:edit:address")));
        rows.add(row(btn("✏️ Деятельность", "P:edit:about")));
        rows.add(row(btn("🔙 Назад", "M:menu")));
        return new InlineKeyboardMarkup(rows);
    }

    // В KeyboardFactory
    public InlineKeyboardMarkup bookingNameChoice(String tgName, String backTarget) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(
                InlineKeyboardButton.builder()
                        .text("👤 " + tgName)
                        .callbackData("B:name:use")       // <--- ВАЖНО: B:
                        .build()
        ));
        rows.add(row(
                InlineKeyboardButton.builder()
                        .text("◀️ Назад")
                        .callbackData("B:back:" + backTarget)
                        .build()
        ));
        return new InlineKeyboardMarkup(rows);
    }


}
