/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;

public class MailjetClientHelper {
  public static MailjetClient createMailjetClient(String mailjetApiKey, String mailjetSecretKey) {
    return new MailjetClient(
        ClientOptions.builder().apiKey(mailjetApiKey).apiSecretKey(mailjetSecretKey).build());
  }
}
