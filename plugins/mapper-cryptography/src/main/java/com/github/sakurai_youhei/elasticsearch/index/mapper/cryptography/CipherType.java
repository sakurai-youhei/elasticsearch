/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.Supplier;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public enum CipherType {
    XOR(Xor::new),
    RSA(Rsa::new);

    final Supplier<Cipher> cipher;

    CipherType(Supplier<Cipher> cipher) {
        this.cipher = cipher;
    }

    public Cipher init(byte[] key) {
        Cipher c = cipher.get();
        c.init(key);
        return c;
    }

    abstract static class Cipher {
        public abstract byte[] encrypt(byte[] input);

        public abstract byte[] decrypt(byte[] input);

        abstract void init(byte[] key);
    }

    static class Xor extends Cipher {
        private byte[] key = new byte[] {};

        @Override
        public byte[] encrypt(byte[] input) {
            byte[] output = new byte[input.length];
            for (int i = 0; i < input.length; i++) {
                output[i] = (byte) (input[i] ^ key[i % key.length]);
            }
            return output;
        }

        @Override
        public byte[] decrypt(byte[] input) {
            return encrypt(input);
        }

        @Override
        void init(byte[] key) {
            this.key = key;
        }
    }

    static class Rsa extends Cipher {
        private java.security.Key key = null;

        private byte[] doFinal(byte[] input, int mode) {
            if (this.key == null) {
                throw new IllegalStateException("Key not initialized");
            }
            try {
                javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
                cipher.init(mode, this.key);
                return cipher.doFinal(input);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                throw new IllegalArgumentException(e);
            } catch (InvalidKeyException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public byte[] encrypt(byte[] input) {
            return doFinal(input, javax.crypto.Cipher.ENCRYPT_MODE);
        }

        @Override
        public byte[] decrypt(byte[] input) {
            return doFinal(input, javax.crypto.Cipher.DECRYPT_MODE);
        }

        @Override
        void init(byte[] key) {
            try {
                try {
                    this.key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
                } catch (InvalidKeySpecException e) {
                    this.key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
                }
            } catch (InvalidKeySpecException e) {
                throw new IllegalArgumentException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
