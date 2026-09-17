package io.github.r0x4nk.nexnote.fileimport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** A real foreign-package sender, using only framework classes in its own process. */
public class ExternalTaskSenderActivity extends Activity {
    private Intent pending;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        read(getIntent());
    }
    @Override public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        read(intent);
    }
    @SuppressWarnings("deprecation")
    private void read(Intent intent) {
        if (intent.getBooleanExtra("finish_sender", false)) {
            finishAndRemoveTask();
        } else {
            pending = intent.getParcelableExtra("forward_intent");
        }
    }
    @Override public void onResume() {
        super.onResume();
        if (pending != null) {
            // Create the outgoing intent here, as a real sharing app does. Reusing a
            // nested cross-UID Intent correctly trips Android 16 redirect hardening.
            Intent outbound = new Intent();
            outbound.setAction(pending.getAction());
            outbound.setComponent(pending.getComponent());
            outbound.setDataAndType(pending.getData(), pending.getType());
            outbound.setFlags(pending.getFlags());
            outbound.replaceExtras(pending.getExtras());
            outbound.setClipData(pending.getClipData());
            pending = null;
            startActivity(outbound);
        }
    }
}
