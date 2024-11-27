package org.nowstart.nyangnyangbot;

import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChzzkLikeBotApplicationTests {
    private static final AES256TextEncryptor textEncryptor = new AES256TextEncryptor();

    @Test
    void testEncryptionDecryption() {
        String password = "";
        String originalText = "";
        textEncryptor.setPassword(password);
        String encryptedText = textEncryptor.encrypt(originalText);
        String decryptedText = textEncryptor.decrypt(encryptedText);

        System.out.println("ENC(" + encryptedText + ")");
        assertEquals(originalText, decryptedText);
    }
}