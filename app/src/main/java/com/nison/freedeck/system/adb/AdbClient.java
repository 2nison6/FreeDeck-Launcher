package com.nison.freedeck.system.adb;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * The smallest possible client for the TV's own debugging service (adbd on this device, port 5555), enough to run a
 * single shell command. Used once, to let the app grant itself the permission it needs; the TV shows its usual
 * "Allow USB debugging?" question the first time.
 */
public final class AdbClient implements AutoCloseable {

    private static final int A_CNXN = 0x4e584e43;
    private static final int A_AUTH = 0x48545541;
    private static final int A_OPEN = 0x4e45504f;
    private static final int A_OKAY = 0x59414b4f;
    private static final int A_CLSE = 0x45534c43;
    private static final int A_WRTE = 0x45545257;

    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;

    private static final int VERSION = 0x01000000;
    private static final int MAX_DATA = 4096;
    private static final int HEADER_SIZE = 24;

    /** How long the user gets to answer the TV's "Allow USB debugging?" question. */
    private static final int ANSWER_TIMEOUT_MILLIS = 60_000;
    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;

    /** DER prefix of a SHA-1 digest: adb signs the token as if it already were a hash. */
    private static final byte[] SHA1_PREFIX = {
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};

    private final Socket socket = new Socket();
    private final AdbKey key;
    private DataInputStream in;
    private OutputStream out;

    private AdbClient(AdbKey key) {
        this.key = key;
    }

    /**
     * Connects to this TV's debugging service and runs {@code command} in its shell.
     *
     * @return everything the command printed (empty when a permission grant succeeds)
     * @throws IOException when the service is off, unreachable, or the user did not allow the connection
     */
    static String shell(AdbKey key, String host, int port, String command) throws IOException, GeneralSecurityException {
        try (AdbClient client = new AdbClient(key)) {
            client.connect(host, port);
            return client.run(command);
        }
    }

    private void connect(String host, int port) throws IOException, GeneralSecurityException {
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
        socket.setSoTimeout(ANSWER_TIMEOUT_MILLIS);
        in = new DataInputStream(socket.getInputStream());
        out = socket.getOutputStream();

        send(A_CNXN, VERSION, MAX_DATA, "host::\0".getBytes(StandardCharsets.UTF_8));
        boolean signed = false;
        while (true) {
            Message reply = read();
            if (reply.command == A_CNXN) {
                return;
            }
            if (reply.command != A_AUTH || reply.arg0 != AUTH_TOKEN) {
                throw new IOException("Unexpected reply while connecting");
            }
            if (!signed) {
                // First try the key the TV may already trust (the user chose "always allow" before).
                send(A_AUTH, AUTH_SIGNATURE, 0, sign(reply.data));
                signed = true;
            } else {
                // Not trusted yet: offer the key, which makes the TV ask the user.
                byte[] publicKey = Base64.getEncoder().encode(key.androidPublicKey());
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                body.write(publicKey);
                body.write(" freedeck@launcher\0".getBytes(StandardCharsets.UTF_8));
                send(A_AUTH, AUTH_RSAPUBLICKEY, 0, body.toByteArray());
            }
        }
    }

    private String run(String command) throws IOException {
        int localId = 1;
        send(A_OPEN, localId, 0, ("shell:" + command + "\0").getBytes(StandardCharsets.UTF_8));
        int remoteId = 0;
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (true) {
            Message message = read();
            if (message.command == A_OKAY) {
                remoteId = message.arg0;
            } else if (message.command == A_WRTE) {
                result.write(message.data);
                send(A_OKAY, localId, message.arg0, new byte[0]);
            } else if (message.command == A_CLSE) {
                if (remoteId != 0) {
                    send(A_CLSE, localId, remoteId, new byte[0]);
                }
                return result.toString(StandardCharsets.UTF_8.name()).trim();
            }
        }
    }

    private byte[] sign(byte[] token) throws GeneralSecurityException {
        byte[] block = new byte[SHA1_PREFIX.length + token.length];
        System.arraycopy(SHA1_PREFIX, 0, block, 0, SHA1_PREFIX.length);
        System.arraycopy(token, 0, block, SHA1_PREFIX.length, token.length);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key.privateKey);
        return cipher.doFinal(block);
    }

    private void send(int command, int arg0, int arg1, byte[] data) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(command).putInt(arg0).putInt(arg1).putInt(data.length).putInt(checksum(data)).putInt(~command);
        out.write(header.array());
        out.write(data);
        out.flush();
    }

    private Message read() throws IOException {
        byte[] raw = new byte[HEADER_SIZE];
        in.readFully(raw);
        ByteBuffer header = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        Message message = new Message();
        message.command = header.getInt();
        message.arg0 = header.getInt();
        message.arg1 = header.getInt();
        int length = header.getInt();
        if (length < 0 || length > MAX_DATA * 64) {
            throw new IOException("Unreasonable message size");
        }
        message.data = new byte[length];
        in.readFully(message.data);
        return message;
    }

    private static int checksum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += b & 0xff;
        }
        return sum;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private static final class Message {
        int command;
        int arg0;
        int arg1;
        byte[] data;
    }
}
