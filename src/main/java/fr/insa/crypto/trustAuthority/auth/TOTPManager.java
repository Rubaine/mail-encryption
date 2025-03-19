package fr.insa.crypto.trustAuthority.auth;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.exceptions.QrGenerationException;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Gestionnaire de l'authentification TOTP (Time-based One-Time Password)
 * Compatible avec Google Authenticator
 */
public class TOTPManager {
    private static final int SECRET_LENGTH = 32;
    private static final String ISSUER = "MessengerSecure";
    
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(SECRET_LENGTH);
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    
    /**
     * Génère un nouveau secret pour Google Authenticator
     * @return Le secret généré
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }
    
    /**
     * Vérifie si le code TOTP fourni est valide pour le secret donné
     * @param code Code TOTP à vérifier
     * @param secret Secret utilisé pour générer le code
     * @return true si le code est valide
     */
    public boolean verifyCode(String code, String secret) {
        if (code == null || secret == null) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code);
    }
    
    /**
     * Génère un URI de données contenant un QR code pour Google Authenticator
     * @param email Adresse email de l'utilisateur (label)
     * @param secret Secret TOTP
     * @return URI de données du QR code (data:image/png;base64,...)
     * @throws QrGenerationException Si la génération du QR code échoue
     */
    public String generateQrCodeUri(String email, String secret) throws QrGenerationException {
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        
        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] imageData = qrGenerator.generate(qrData);
        
        return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
    }
}
