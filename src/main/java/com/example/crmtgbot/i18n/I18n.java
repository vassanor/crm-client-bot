package com.example.crmtgbot.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class I18n {

    private final MessageSource messageSource;

    public String t(String code, Object... args) {
        return messageSource.getMessage(code, args, Locale.forLanguageTag("ru"));
    }
}
