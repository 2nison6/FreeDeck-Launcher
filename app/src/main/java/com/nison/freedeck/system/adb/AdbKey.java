package com.nison.freedeck.system.adb;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;

/** The RSA key this app identifies itself with to the TV's debugging service, and its "Android public key" encoding. */
final class AdbKey {

    private static final int KEY_BITS = 2048;
    private static final int KEY_BYTES = KEY_BITS / 8;
    private static final int KEY_WORDS = KEY_BYTES / 4;

    final PrivateKey privateKey;
    final BigInteger modulus;
    final BigInteger publicExponent;

    private AdbKey(PrivateKey privateKey, BigInteger modulus, BigInteger publicExponent) {
        this.privateKey = privateKey;
        this.modulus = modulus;
        this.publicExponent = publicExponent;
    }

    static AdbKey generate() throws java.security.GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(KEY_BITS);
        KeyPair pair = generator.generateKeyPair();
        return from(pair.getPrivate());
    }

    /** Rebuilds the key from the private half saved earlier ({@link #encoded()}). */
    static AdbKey fromEncoded(byte[] pkcs8) throws java.security.GeneralSecurityException {
        return from(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8)));
    }

    private static AdbKey from(PrivateKey key) throws java.security.GeneralSecurityException {
        RSAPrivateCrtKey crt = (RSAPrivateCrtKey) key;
        return new AdbKey(key, crt.getModulus(), crt.getPublicExponent());
    }

    byte[] encoded() {
        return privateKey.getEncoded();
    }

    /** The public key in the layout adbd expects: length, n0inv, modulus, R^2 mod n, exponent; little-endian throughout. */
    byte[] androidPublicKey() {
        return androidPublicKey(modulus, publicExponent);
    }

    static byte[] androidPublicKey(BigInteger modulus, BigInteger exponent) {
        BigInteger word = BigInteger.ONE.shiftLeft(32);
        BigInteger inverse = modulus.mod(word).modInverse(word);
        int n0inv = word.subtract(inverse).intValue();
        BigInteger rr = BigInteger.ONE.shiftLeft(KEY_BITS * 2).mod(modulus);

        ByteBuffer out = ByteBuffer.allocate(4 + 4 + KEY_BYTES + KEY_BYTES + 4).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(KEY_WORDS);
        out.putInt(n0inv);
        out.put(littleEndian(modulus, KEY_BYTES));
        out.put(littleEndian(rr, KEY_BYTES));
        out.putInt(exponent.intValue());
        return out.array();
    }

    static byte[] littleEndian(BigInteger value, int length) {
        byte[] big = value.toByteArray();
        byte[] out = new byte[length];
        for (int i = 0; i < length && i < big.length; i++) {
            out[i] = big[big.length - 1 - i];
        }
        return out;
    }
}
