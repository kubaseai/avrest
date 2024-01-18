package io.github.kubaseai.av.config;

import java.nio.file.Paths;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

// TODO: https://techdocs.broadcom.com/us/en/symantec-security-software/endpoint-security-and-management/symantec-protection-engine/9-1-0/SPE-Docker-Containers/how-to-scan-files-using-icap.html

@Configuration
@ConfigurationProperties(prefix = "server.ssl")
public class IcapConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    String keyStoreType;
    String keyStore;
    char[] keyStorePassword;
    String keyAlias;
    int icapPort = -1;

    public void setKeyStoreType(String type) {
        this.keyStoreType = type;
    }
    public void setKeystore(String keystore) {
        this.keyStore = keystore;
    }
    public void setKeystorePassword(String password) {
        this.keyStorePassword = password.toCharArray();        
    }
    public void setKeyAlias(String alias) {
        this.keyAlias = alias;
    }
    public void setIcapPort(int port) {
        this.icapPort = port > 0 ? port : -1;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        var handler = new io.github.kubaseai.av.utils.icap.IcapHttpProtocolHandler();
        Connector c = new Connector(handler);
        handler.setSSLEnabled(true);
        handler.setKeystoreType(keyStoreType);
        handler.setKeystoreFile(resolveFile(keyStore));
        handler.setKeyAlias(keyAlias);
        handler.setKeystorePass(new String(keyStorePassword));
        for (int i=0; i < keyStorePassword.length; i++)
            keyStorePassword[i]=0;
        c.setPort(icapPort>0 ? icapPort : 1344);
        c.setScheme("https");
        c.setSecure(true);
        factory.addAdditionalTomcatConnectors(c);
    }
    private String resolveFile(String file) {
        if (file!=null && file.startsWith("classpath:")) {
            return new ClassPathResource(file.substring(10)).getPath();
        }
        return Paths.get(file).toFile().getAbsolutePath();
    }
}