package fr.insa.crypto.encryption;

import it.unisa.dia.gas.jpbc.Element;

public class IBEcipher {
    
    private Element U; // rP (vu dans le cours)
    
    byte[] V; // K xor e(Q_id,P_pub) avec K la clef symmetrique AES
    
    byte[] Aescipher; // résultat du chiffrement avec AES

    public IBEcipher(Element U, byte[] V, byte[] Aescipher) {
        this.U = U;
        this.V = V;
        this.Aescipher = Aescipher;
    }

    public byte[] getAescipher() {
        return Aescipher;
    }

    public Element getU() {
        return U;
    }

    public byte[] getV() {
        return V;
    }
    
    
}
