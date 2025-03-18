/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.insa.crypto.trustAuthority;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

/**
 * @author imino
 */
public class SettingParameters {

    private final Pairing pairing;
    private final Element generator; //generateur
    private final Element publicKey; // clef publique du système
    private final Element masterKey; // clef du maitre

    /**
     * Constructeur par défaut qui initialise tous les paramètres
     */
    public SettingParameters() {
        this.pairing = PairingFactory.getPairing("params/curves/a.properties");
        this.generator = pairing.getG1().newRandomElement(); // choix d'un générateur
        this.masterKey = pairing.getZr().newRandomElement(); //choix de la clef du maitre
        this.publicKey = generator.duplicate().mulZn(masterKey); // calcule de la clef publique du système
    }
    
    /**
     * Constructeur alternatif pour les sous-classes qui veulent initialiser les paramètres elles-mêmes
     * @param initialize Si false, ne pas initialiser les paramètres (ils seront définis par la sous-classe)
     */
    protected SettingParameters(boolean initialize) {
        if (initialize) {
            this.pairing = PairingFactory.getPairing("params/curves/a.properties");
            this.generator = pairing.getG1().newRandomElement();
            this.masterKey = pairing.getZr().newRandomElement();
            this.publicKey = generator.duplicate().mulZn(masterKey);
        } else {
            this.pairing = null;
            this.generator = null;
            this.masterKey = null;
            this.publicKey = null;
        }
    }

    public Pairing getPairing() {
        return pairing;
    }

    public Element getPublicKey() {
        return publicKey;
    }

    public Element getGenerator() {
        return generator;
    }

    public Element getMasterKey() {
        return masterKey;
    }


}
