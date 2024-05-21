package net.atos.zac.mail

import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.PreencodedMimeBodyPart
import net.atos.zac.mail.model.Attachment
import java.nio.charset.StandardCharsets
import java.util.Date

class MailMessageBuilder(
    private var fromAddress: Address,
    private var toAddress: Address,
    private var replyToAddress: Address? = null,
    private var mailSubject: String,
    private var body: String,
    private var attachments: List<Attachment>
) {
    fun build(mailSession: Session) = MimeMessage(mailSession).apply {
        addHeader("Content-type", "text/HTML; charset=${StandardCharsets.UTF_8.name()}")
        addHeader("format", "flowed")
        addHeader("Content-Transfer-Encoding", "8bit")

        setFrom(fromAddress)
        setRecipients(Message.RecipientType.TO, arrayOf(toAddress))
        replyTo = replyToAddress?.let { arrayOf(replyToAddress) }
        subject = mailSubject
        sentDate = Date()

        setContent(
            MimeMultipart().apply {
                addBodyPart(
                    MimeBodyPart().apply {
                        setText(body, StandardCharsets.UTF_8.name(), "html")
                    }
                )
                attachments.forEach {
                    val messageBodyPart = PreencodedMimeBodyPart("base64").apply {
                        setContent(it.base64Content, it.contentType)
                        fileName = it.filename
                    }
                    addBodyPart(messageBodyPart)
                }
            }
        )
    }
}
