package com.nison.freedeck.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.CustomBackground;
import com.nison.freedeck.data.upload.LocalAddress;
import com.nison.freedeck.data.upload.PhoneUploadServer;
import com.nison.freedeck.data.upload.QrCode;

import java.io.File;
import java.io.IOException;

/**
 * Shows an address to open on a phone. While it is open the TV listens for a picture sent from that page, and a
 * picture that arrives becomes the background. Back (or the Cancel button) closes it and stops listening.
 */
class PhoneUploadDialog extends Dialog {

    /** How long a QR code works for; after that a new one has to be generated. */
    private static final long EXPIRY_MS = 5 * 60 * 1000L;

    private final Runnable onSaved;
    private final int slot;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable expire = this::onExpired;
    private long expiresAt;
    private final Runnable tickTimer = new Runnable() {
        @Override
        public void run() {
            long left = Math.max(0, expiresAt - android.os.SystemClock.elapsedRealtime());
            long seconds = (left + 999) / 1000;
            ((TextView) findViewById(R.id.phone_upload_timer)).setText(
                    getContext().getString(R.string.phone_upload_expires_in, seconds / 60, seconds % 60));
            if (left > 0) {
                handler.postDelayed(this, 1000);
            }
        }
    };
    private PhoneUploadServer server;

    PhoneUploadDialog(Context context, int slot, Runnable onSaved) {
        super(context, R.style.OptionsDialog);
        this.slot = slot;
        this.onSaved = onSaved;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.nison.freedeck.ui.common.TextStyler.install(getWindow().getLayoutInflater(), getContext(),
                com.nison.freedeck.data.settings.AppSettings.from(getContext()));
        setContentView(R.layout.dialog_phone_upload);
        TextView address = findViewById(R.id.phone_upload_address);
        TextView status = findViewById(R.id.phone_upload_status);
        findViewById(R.id.phone_upload_cancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.phone_upload_cancel).requestFocus();

        findViewById(R.id.phone_upload_regenerate).setOnClickListener(v -> startServer());
        startServer();
    }

    /** Listens on a brand-new secret address (stopping the previous one) and shows it as text and as a QR code. */
    private void startServer() {
        TextView address = findViewById(R.id.phone_upload_address);
        TextView status = findViewById(R.id.phone_upload_status);
        android.widget.ImageView qr = findViewById(R.id.phone_upload_qr);
        stopServer();
        handler.removeCallbacks(expire);
        handler.removeCallbacks(tickTimer);
        ((TextView) findViewById(R.id.phone_upload_timer)).setText("");
        status.setVisibility(android.view.View.VISIBLE);
        qr.setImageDrawable(null);
        address.setText("");
        String host = LocalAddress.find();
        if (host == null) {
            status.setText(R.string.phone_upload_no_network);
            return;
        }
        server = new PhoneUploadServer(getContext().getCacheDir(), this::onReceived);
        try {
            String path = server.start();
            String url = "http://" + host + ":" + server.port() + path;
            address.setText(url);
            qr.setImageBitmap(QrCode.of(url, getContext().getResources().getDimensionPixelSize(R.dimen.phone_qr_size)));
            status.setText("");
            status.setVisibility(android.view.View.GONE);
            handler.postDelayed(expire, EXPIRY_MS);
            expiresAt = android.os.SystemClock.elapsedRealtime() + EXPIRY_MS;
            tickTimer.run();
        } catch (IOException e) {
            server = null;
            status.setText(R.string.phone_upload_failed);
        }
    }

    /** The QR code is five minutes old: stop listening and say so, until a new one is generated. */
    private void onExpired() {
        handler.removeCallbacks(tickTimer);
        ((TextView) findViewById(R.id.phone_upload_timer)).setText("");
        stopServer();
        ((android.widget.ImageView) findViewById(R.id.phone_upload_qr)).setImageDrawable(null);
        ((TextView) findViewById(R.id.phone_upload_address)).setText("");
        TextView status = findViewById(R.id.phone_upload_status);
        status.setText(R.string.phone_upload_expired);
        status.setVisibility(android.view.View.VISIBLE);
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /** A picture has arrived (on the server's thread): shrink and keep it, then tell the screen. */
    private void onReceived(File picture) {
        Context context = getContext();
        boolean saved = CustomBackground.save(context, slot, Uri.fromFile(picture));
        picture.delete();
        findViewById(R.id.phone_upload_cancel).post(() -> {
            if (!saved) {
                Toast.makeText(context, R.string.background_add_failed, Toast.LENGTH_LONG).show();
                ((TextView) findViewById(R.id.phone_upload_status)).setText(R.string.phone_upload_try_again);
                return;
            }
            showSuccess();
        });
    }

    /** Swaps the QR screen for a tick and counts down 5 to 1, then closes and returns to Settings. */
    private void showSuccess() {
        handler.removeCallbacks(expire);
        handler.removeCallbacks(tickTimer);
        findViewById(R.id.phone_upload_timer).setVisibility(android.view.View.GONE);
        stopServer();
        setCancelable(false);
        ((TextView) findViewById(R.id.phone_upload_title)).setText(R.string.phone_upload_done);
        android.widget.ImageView tick = findViewById(R.id.phone_upload_qr);
        tick.setImageResource(R.drawable.ic_check);
        findViewById(R.id.phone_upload_address).setVisibility(android.view.View.GONE);
        findViewById(R.id.phone_upload_status).setVisibility(android.view.View.GONE);
        findViewById(R.id.phone_upload_buttons).setVisibility(android.view.View.GONE);
        TextView steps = findViewById(R.id.phone_upload_steps);
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        int[] left = {5};
        Runnable tickDown = new Runnable() {
            @Override
            public void run() {
                if (left[0] == 0) {
                    dismiss();
                    onSaved.run();
                    return;
                }
                steps.setText(getContext().getString(R.string.phone_upload_redirect, left[0]--));
                handler.postDelayed(this, 1000);
            }
        };
        tickDown.run();
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(expire);
        handler.removeCallbacks(tickTimer);
        stopServer();
        super.onStop();
    }
}
