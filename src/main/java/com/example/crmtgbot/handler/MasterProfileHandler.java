package com.example.crmtgbot.handler;

import com.example.crmtgbot.bot.BotIO;
import com.example.crmtgbot.i18n.I18n;
import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.service.KeyboardFactory;
import com.example.crmtgbot.service.MasterService;
import com.example.crmtgbot.service.SessionStore;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
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
            // ожидаем текст, если активен один из шагов ввода
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
                    String tgName = buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_CHOICE);
                    st.setActive(true);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.name.ask"), kf.profileNameChoice(tgName));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "M:menu" -> {
                    // просто открываем меню мастера
                    Master m = masterService.getOrCreateMaster(chatId, cq.getFrom().getId().intValue(), cq.getFrom().getUserName(),
                            buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName()));
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("master.menu"), kf.masterMenu(m.isAutoConfirm()));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:name:use" -> {
                    st.setName(buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName()));
                    st.setStep(SessionStore.MasterProfileState.Step.ADDRESS_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.address.ask"), kf.profileAskAddress());
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:name:input" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), "✍️ Отправьте имя одним сообщением", null);
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:address:input" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.ADDRESS_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.address.ask"), null);
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:about:input" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.name.input.hint"), null);
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }

                // back-навигация (упрощённо)
                case "P:back:name" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_CHOICE);
                    String tgName = buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.name.ask"), kf.profileNameChoice(tgName));
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:back:address" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.ADDRESS_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.address.ask"), kf.profileAskAddress());
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:back:about" -> {
                    st.setStep(SessionStore.MasterProfileState.Step.ABOUT_INPUT);
                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.about.ask"), kf.profileAskAbout());
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:save" -> {
                    Master m = masterService.getOrCreateMaster(chatId, cq.getFrom().getId().intValue(), cq.getFrom().getUserName(),
                            st.getName() != null ? st.getName() : buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName()));
                    m.setDisplayName(st.getName());
                    m.setAddress(st.getAddress());
                    m.setAbout(st.getAbout());
                    masterService.save(m); // <--- вот так

                    io.edit(chatId, cq.getMessage().getMessageId(), i18n.t("profile.saved"), kf.masterMenu(m.isAutoConfirm()));
                    sessions.clearProfile(chatId);
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }
                case "P:start" -> {
                    st.setActive(true);
                    st.setStep(SessionStore.MasterProfileState.Step.NAME_CHOICE);
                    String tgName = buildTgName(cq.getFrom().getFirstName(), cq.getFrom().getLastName(), cq.getFrom().getUserName());
                    io.edit(chatId, cq.getMessage().getMessageId(),
                            i18n.t("profile.name.ask"),
                            kf.profileNameChoice(tgName)
                    );
                    io.answerCallback(cq.getId(), "OK", false);
                    return;
                }

            }

        }

        // Текстовый ввод
        if (u.hasMessage() && u.getMessage().hasText()) {
            Long chatId = u.getMessage().getChatId();
            String text = u.getMessage().getText().trim();
            var st = sessions.ensureProfile(chatId);
            switch (st.getStep()) {
                case NAME_INPUT -> {
                    st.setName(text);
                    st.setStep(SessionStore.MasterProfileState.Step.ADDRESS_INPUT);
                    io.send(chatId, i18n.t("profile.address.ask"), kf.profileAskAddress());
                }
                case ADDRESS_INPUT -> {
                    st.setAddress(text);
                    st.setStep(SessionStore.MasterProfileState.Step.ABOUT_INPUT);
                    io.send(chatId, i18n.t("profile.about.ask"), kf.profileAskAbout());
                }
                case ABOUT_INPUT -> {
                    st.setAbout(text);
                    st.setStep(SessionStore.MasterProfileState.Step.PREVIEW);
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
