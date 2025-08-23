// src/main/java/com/example/crmtgbot/handler/MasterProfileHandler.java
package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.service.KeyboardFactory;
import com.example.crmtgbot.service.MasterService;
import com.example.crmtgbot.service.SessionStore;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class MasterProfileHandler implements UpdateHandler {

    private final SessionStore sessions;
    private final MasterService masterService;
    private final KeyboardFactory kf;
    private final BotIO io;
    private final I18n i18n;

    @Override
    public boolean canHandle(Update u) {
        if (u.hasCallbackQuery()) {
            String data = u.getCallbackQuery().getData();
            return data != null && (data.startsWith("P:") || data.equals("M:profile") || data.equals("M:menu"));
        }
        if (u.hasMessage() && u.getMessage().hasText()) {
            Long chatId = u.getMessage().getChatId();
            return sessions.getProfile(chatId)
                    .map(SessionStore.MasterProfileState::isActive)
                    .orElse(false);
        }
        return false;
    }

    @Override
    public void handle(Update u) {
        if (u.hasCallbackQuery()) {
            CallbackQuery cq = u.getCallbackQuery();
            Long chatId = cq.getMessage().getChatId();
            String data = cq.getData();
            var st = sessions.ensureProfile(chatId);

            switch (data) {
                case "M:profile" -> {
                    // вход в мастер-профиль
                    String tgName = buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                    st.setActive(true);
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_CHOICE);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.name.ask"), kf.profileNameChoice(tgName));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "M:menu" -> {
                    Master m = masterService.getOrCreateMaster(chatId, cq.getFrom().getId(), cq.getFrom().getUserName(),
                            buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName()));
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("master.menu"), kf.masterMenu(m.isAutoConfirm()));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:start" -> {
                    // с welcome-экрана
                    String tgName = buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                    st.setActive(true);
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_CHOICE);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.name.ask"), kf.profileNameChoice(tgName));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:name:use" -> {
                    st.setName(buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName()));
                    st.setStep(SessionStore.MasterProfileState.Step.ADDRESS_INPUT);
                    // ждём текст адреса, показываем только «Назад»
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.address.ask"), kf.backTo("P:back:name"));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }


                // back-навигация
                case "P:back:name" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_CHOICE);
                    String tgName = buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.name.ask"), kf.profileNameChoice(tgName));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:back:address" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.ADDRESS_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.address.ask"), kf.backTo("P:back:name"));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:back:about" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.ABOUT_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.about.ask"), kf.backTo("P:back:address"));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:save" -> {
                    // сохраняем и выходим в меню
                    var from = cq.getFrom();
                    Master m = masterService.getOrCreateMaster(chatId, from.getId(), from.getUserName(),
                            st.getName() != null ? st.getName() : buildTgName(from.getFirstName(), from.getLastName(), from.getUserName()));
                    m.setDisplayName(st.getName());
                    m.setAddress(st.getAddress());
                    m.setAbout(st.getAbout());
                    masterService.save(m);

                    sessions.clearProfile(chatId);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.saved"), kf.masterMenu(m.isAutoConfirm()));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
            }

        }

        // Текстовые ответы по шагам
        if (u.hasMessage() && u.getMessage().hasText()) {
            Long chatId = u.getMessage().getChatId();
            String text = u.getMessage().getText().trim();
            var st = sessions.ensureProfile(chatId);

            switch (st.getStep()) {
                case NAME_CHOICE, NAME_INPUT -> {
                    // пользователь просто написал имя
                    st.setName(text);
                    st.setStep(SessionStore.MasterProfileState.Step.ADDRESS_INPUT);
                    io.send(chatId, i18n.t("profile.address.ask"), kf.backTo("P:back:name"));
                }
                case ADDRESS_INPUT -> {
                    st.setAddress(text);
                    st.setStep(SessionStore.MasterProfileState.Step.ABOUT_INPUT);
                    io.send(chatId, i18n.t("profile.about.ask"), kf.backTo("P:back:address"));
                }
                case ABOUT_INPUT -> {
                    st.setAbout(text);
                    st.setStep(SessionStore.MasterProfileState.Step.PREVIEW);
                    st.setActive(false); // на предпросмотре больше не принимаем произвольный текст
                    String preview = i18n.t("profile.preview",
                            st.getName() == null ? "—" : st.getName(),
                            st.getAddress() == null ? "—" : st.getAddress(),
                            st.getAbout() == null ? "—" : st.getAbout());
                    io.send(chatId, preview, kf.profilePreviewSave());
                }
                default -> { /* ignore */ }
            }
        }
    }

    private String buildTgName(String first, String last, String username) {
        return getString(first, last, username);
    }

    @NotNull
    static String getString(String first, String last, String username) {
        String display = ((first == null ? "" : first) + (last == null ? "" : " " + last)).trim();
        if (display.isBlank() && username != null) display = "@" + username;
        if (display.isBlank()) display = "Гость";
        return display;
    }
}
