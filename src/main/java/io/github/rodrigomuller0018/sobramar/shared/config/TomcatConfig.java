package io.github.rodrigomuller0018.sobramar.shared.config;

import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TomcatConfig {

    /** O padrão do Tomcat recusa mais de 10 idiomas, e navegadores configurados com muitos idiomas passam disso. */
    private static final int MAXIMO_DE_IDIOMAS = 50;

    @Bean
    TomcatConnectorCustomizer aceitarNavegadoresComMuitosIdiomas() {
        return conector -> conector.setMaxLocaleCount(MAXIMO_DE_IDIOMAS);
    }
}
