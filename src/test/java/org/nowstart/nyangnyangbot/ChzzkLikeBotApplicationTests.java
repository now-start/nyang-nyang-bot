package org.nowstart.nyangnyangbot;

import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChzzkLikeBotApplicationTests {

    private static final AES256TextEncryptor textEncryptor = new AES256TextEncryptor();

    @Test
    void testEncryptionDecryption() {
        String password = "";
        String originalText = "";

        String encryptedText = "";
        String decryptedText = "";

        textEncryptor.setPassword(password);
        try {
            encryptedText = textEncryptor.encrypt(originalText);
            decryptedText = textEncryptor.decrypt(originalText);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("ENC(" + encryptedText + ")");
        System.out.println(decryptedText);

        assertThat(originalText).isNotNull();
    }
}