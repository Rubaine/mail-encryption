package fr.insa.crypto.encryption;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;

import java.util.Base64;

/**
 * Classe pour la génération des clés dans le système IBE
 */
public class KeyGeneration {
    private Pairing pairing;
    private PairingParameters parameters;
    
    public KeyGeneration(Pairing pairing, PairingParameters parameters) {
        this.pairing = pairing;
        this.parameters = parameters;
    }
    
    /**
     * Génère une clé de session aléatoire pour le chiffrement symétrique
     * @return Une clé de session encodée en Base64
     */
    public String generateSessionKey() {
        // Génération d'une clé aléatoire dans Zr
        Element sessionKey = pairing.getZr().newRandomElement().getImmutable();
        return Base64.getEncoder().encodeToString(sessionKey.toBytes());
    }
    
    /**
     * Convertit une clé privée encodée en Base64 en élément JPBC
     */
    public Element decodePrivateKey(String encodedPrivateKey) {
        byte[] privateKeyBytes = Base64.getDecoder().decode(encodedPrivateKey);
        return pairing.getG1().newElementFromBytes(privateKeyBytes).getImmutable();
    }
    
    /**
     * Calcule l'élément Q_ID à partir d'une adresse email (identité)
     */
    public Element calculateQid(String email) {
        return pairing.getG1().newElementFromHash(email.getBytes(), 0, email.length()).getImmutable();
    }
}
