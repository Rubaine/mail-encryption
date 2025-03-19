package fr.insa.crypto.encryption;

import fr.insa.crypto.trustAuthority.SettingParameters;
import it.unisa.dia.gas.jpbc.Element;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Classe gérant le chiffrement basé sur l'identité, adaptée du code fonctionnel
 */
public class IdentityBasedEncryption {
    private final SettingParameters parameters;

    public IdentityBasedEncryption(SettingParameters parameters) {
        this.parameters = parameters;
    }

    public IBEcipher IBEencryption(byte[] message, String pk) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchPaddingException, IllegalBlockSizeException, UnsupportedEncodingException, BadPaddingException, InvalidKeyException {
        // methode de chiffrement BasicID

        Element aeskey = this.parameters.getPairing().getGT().newRandomElement(); //choix de la clef symmetrique AES

        byte[] bytes = pk.getBytes(); // transformation de la clef publique (id) au format binaire

        Element r = this.parameters.getPairing().getZr().newRandomElement(); // nombre aléatoire choisi dans Z_r

        Element U = this.parameters.getGenerator().duplicate().mulZn(r); // rP (dans le slide du cours)

        Element Q_id = this.parameters.getPairing().getG1().newElementFromHash(bytes, 0, bytes.length); // H_1(id) (dans le slide du cours)

        Element pairingresult = this.parameters.getPairing().pairing(Q_id, this.parameters.getPublicKey()); //e(Q_id,P_pub) dans le slide du cours

        System.out.println("before pairing result:" + pairingresult);

        pairingresult.powZn(r);

        System.out.println("after pairing result:" + pairingresult);

        byte[] V = EncryptionUtils.xor(aeskey.toBytes(), pairingresult.toBytes()); //K xor e(Q_id,P_pub)^r

        byte[] Aescipher = AESCrypto.encrypt(message, aeskey.toBytes());  // chiffrement AES

        return new IBEcipher(U, V, Aescipher); //instaciation d'un objet representant un ciphertext hybride combinant (BasicID et AES)
    }


    public byte[] IBEdecryption(Element sk, IBEcipher C) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        //Déchiffrement IBE

        Element pairingresult = this.parameters.getPairing().pairing(sk, C.getU()); //e(d_id,U) dans le slide du cours avec d_id= la clef  privée de l'utilisateur

        byte[] resultingAeskey = EncryptionUtils.xor(C.getV(), pairingresult.toBytes());  // V xor H_2(e(d_id,U))=K avec K est la clef symmetrique AES

        return AESCrypto.decrypt(C.getAescipher(), resultingAeskey); // déchiffrement AES
    }

    public SettingParameters getParameters() {
        return parameters;
    }
}
