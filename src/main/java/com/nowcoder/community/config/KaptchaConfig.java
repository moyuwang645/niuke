package com.nowcoder.community.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {
    @Bean
    public Producer kaptchaProducer() {
        Properties prop = new Properties();
        prop.setProperty("kaptcha.image.width", "100");
        prop.setProperty("kaptcha.image.height", "50");
        prop.setProperty("kaptcha.textproducer.char.length", "4");
        prop.setProperty("kaptcha.textproducer.font.color", "black");
        prop.setProperty("kaptcha.textproducer.char.size", "32");
        prop.setProperty("kaptcha.textproducer.char.string", "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        prop.setProperty("kaptcha.textproducer.char.noise", "com.google.code.kaptcha.impl.NoNoise");
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Config config = new Config(prop);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
