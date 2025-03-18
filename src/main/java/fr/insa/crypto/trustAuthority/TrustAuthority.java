package fr.insa.crypto.trustAuthority;

import it.unisa.dia.gas.jpbc.Element;

/**
 * Autorité de confiance pour le système de chiffrement basé sur l'identité
 * Adaptée pour utiliser l'approche du code fonctionnel
 */
public class TrustAuthority {
    private final SettingParameters parameters;
    private final KeyDistributor keyDistributor;


    public TrustAuthority() {
        // Initialisation du système
        this.parameters = new SettingParameters();
        this.keyDistributor = new KeyDistributor(this);
    }

    /**
     * Génère la clé privée pour une identité donnée
     * suivant le schéma BasicIdent: dID = sQID
     */
    public KeyPair generatePrivateKey(String id) {

        byte[] bytes = id.getBytes(); // représentation de l'id sous format binaire
        Element Q_id = this.parameters.getPairing().getG1().newElementFromHash(bytes, 0, bytes.length); //H_1(id)
        Element sk = Q_id.duplicate().mulZn(this.parameters.getMasterKey());

        return new KeyPair(id, sk);
    }

    public SettingParameters getParameters() {
        return parameters;
    }

    public KeyDistributor getKeyDistributor() {
        return keyDistributor;
    }
}
