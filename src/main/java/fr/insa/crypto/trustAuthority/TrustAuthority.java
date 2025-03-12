package fr.insa.crypto.trustAuthority;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Autorité de confiance pour le système de chiffrement basé sur l'identité
 */
public class TrustAuthority {
    private Pairing pairing;
    private Element masterSecret; // Clé maître secrète
    private Element publicKey;    // Clé publique du système
    private PairingParameters parameters;
    private KeyDistributor keyDistributor;
    
    // Stockage temporaire des clés privées générées (dans un système réel, on utiliserait une base de données)
    private Map<String, Element> privateKeys = new HashMap<>();
    
    public TrustAuthority() {
        // Initialisation du système
        initializeSystem();
        this.keyDistributor = new KeyDistributor(this);
    }
    
    private void initializeSystem() {
        // Génération des paramètres de courbe Type A (pour le pairing)
        int rBits = 160; // Taille de l'ordre du groupe
        int qBits = 512; // Taille du champ premier
        
        TypeACurveGenerator generator = new TypeACurveGenerator(rBits, qBits);
        parameters = generator.generate();
        
        // Initialisation du pairing avec les paramètres
        pairing = PairingFactory.getPairing("params/curves/a.properties");
        PairingFactory.getInstance().setUsePBCWhenPossible(true);
        
        // Génération de la clé maître et de la clé publique
        masterSecret = pairing.getZr().newRandomElement().getImmutable();
        publicKey = pairing.getG1().newRandomElement().powZn(masterSecret).getImmutable();
    }
    
    public Element generatePrivateKey(String emailId) {
        // Hash de l'identité en un élément du groupe G1
        Element hashedId = hashIdentity(emailId);
        
        // Calcul de la clé privée: d_ID = s * Q_ID (s est la clé maître, Q_ID est l'identité hachée)
        Element privateKey = hashedId.duplicate().mulZn(masterSecret).getImmutable();
        
        // Stockage de la clé
        privateKeys.put(emailId, privateKey);
        
        return privateKey;
    }
    
    private Element hashIdentity(String identity) {
        // Dans un système réel, utiliser une fonction de hachage appropriée (H1)
        // pour mapper l'identité à un point sur la courbe elliptique (élément de G1)
        return pairing.getG1().newElementFromHash(identity.getBytes(), 0, identity.length()).getImmutable();
    }
    
    // Getters pour les paramètres nécessaires aux opérations
    public Pairing getPairing() {
        return pairing;
    }
    
    public Element getPublicKey() {
        return publicKey;
    }
    
    public PairingParameters getParameters() {
        return parameters;
    }
    
    public KeyDistributor getKeyDistributor() {
        return keyDistributor;
    }
}
