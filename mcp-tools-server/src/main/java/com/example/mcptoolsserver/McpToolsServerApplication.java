package com.example.mcptoolsserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpToolsServerApplication {

    public static void main(String[] args) {

        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {

            System.setProperty("javax.net.ssl.trustStoreType", "WINDOWS-ROOT");
        }

        SpringApplication.run(McpToolsServerApplication.class, args);
    }

}
