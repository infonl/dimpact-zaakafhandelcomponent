package net.atos.zac.mail;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;

public class MailjetClientHelper {
    public static MailjetClient createMailjetClient(String mailjetApiKey, String mailjetSecretKey) {
        return new MailjetClient(
                                 ClientOptions.builder()
                                              .apiKey(mailjetApiKey)
                                              .apiSecretKey(mailjetSecretKey)
                                              .build()
        );
    }
}
