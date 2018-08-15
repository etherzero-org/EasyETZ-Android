package com.etzwallet.tools.security;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

import com.etzwallet.R;
import com.etzwallet.presenter.activities.DisabledActivity;
import com.etzwallet.presenter.activities.util.ActivityUTILS;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.fragments.FragmentFingerprint;
import com.etzwallet.presenter.fragments.FragmentPin;
import com.etzwallet.presenter.interfaces.BRAuthCompletion;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;

import java.util.concurrent.TimeUnit;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/20/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class AuthManager {
    public static final String TAG = AuthManager.class.getName();
    private static AuthManager instance;
    private String previousTry;
    private static final int LOCK_FAIL_ATTEMPT_COUNT = 3;
    private static final int PIN_DIGITS = 6;


    private AuthManager() {
        previousTry = "";
    }

    public static AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public boolean checkAuth(CharSequence passSequence, Context context) {
        Log.e(TAG, "checkAuth: ");
        String tempPass = passSequence.toString();
        if (!previousTry.equals(tempPass)) {
            int failCount = BRKeyStore.getFailCount(context);
            BRKeyStore.putFailCount(failCount + 1, context);
        }
        previousTry = tempPass;

        String pass = BRKeyStore.getPinCode(context);
        boolean match = pass != null && tempPass.equals(pass);
        if (!match) {
            if (BRKeyStore.getFailCount(context) >= LOCK_FAIL_ATTEMPT_COUNT) {
                setWalletDisabled((Activity) context);
            }
        }

        return match;
    }

    //when pin auth success
    public void authSuccess(final Context app) {
        //put the new total limit in 1 seconds, leave some time for the core to register any new tx
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(DateUtils.SECOND_IN_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
                if (wm != null) {
                    BRKeyStore.putTotalLimit(app, wm.getTotalSent(app).add(BRKeyStore.getSpendLimit(app, wm.getIso())), wm.getIso());
                }
            }
        });

        BRKeyStore.putFailCount(0, app);
        BRKeyStore.putLastPinUsedTime(System.currentTimeMillis(), app);
    }

    public void authFail(Context app) {

    }

    public boolean isWalletDisabled(Activity app) {
        long start = System.currentTimeMillis();
        int failCount = BRKeyStore.getFailCount(app);
        return failCount >= LOCK_FAIL_ATTEMPT_COUNT && disabledUntil(app) > BRSharedPrefs.getSecureTime(app);

    }

    public long disabledUntil(Activity app) {
        int failCount = BRKeyStore.getFailCount(app);
        long failTimestamp = BRKeyStore.getFailTimeStamp(app);
        double pow = Math.pow(PIN_DIGITS, failCount - LOCK_FAIL_ATTEMPT_COUNT) * (DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);
        return (long) ((failTimestamp + pow * DateUtils.SECOND_IN_MILLIS));
    }

    public void setWalletDisabled(Activity app) {
        if (!(app instanceof DisabledActivity)) {
            ActivityUTILS.showWalletDisabled(app);
        }
    }

    public void setPinCode(String pass, Activity context) {
        BRKeyStore.putFailCount(0, context);
        BRKeyStore.putPinCode(pass, context);
        BRKeyStore.putLastPinUsedTime(System.currentTimeMillis(), context);
    }

    public void updateDots(Context context, int pinLimit, String pin, View dot1, View dot2, View dot3, View dot4, View dot5, View dot6,
                           int emptyPinRes, final OnPinSuccess onPinSuccess) {
        if (dot1 == null || context == null) {
            return;
        }
        int selectedDots = pin.length();

        if (pinLimit == PIN_DIGITS) {
            dot6.setVisibility(View.VISIBLE);
            dot1.setVisibility(View.VISIBLE);
            dot1.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
            selectedDots--;
        } else {
            dot6.setVisibility(View.GONE);
            dot1.setVisibility(View.GONE);
        }

        dot2.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot3.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot4.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot5.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        if (pinLimit == 6) {
            selectedDots--;
            dot6.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        }

        if (pin.length() == pinLimit) {
            onPinSuccess.onSuccess();

        }
    }

    public void authPrompt(final Context context, String title, String message, boolean forcePin, boolean forceFingerprint, BRAuthCompletion completion) {
        Log.i(TAG, "authPrompt: message=="+message);

        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "authPrompt: context is null or not Activity: " + context);
            return;
        }
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);

        boolean useFingerPrint = isFingerPrintAvailableAndSetup(context);

        long passTime = BRKeyStore.getLastPinUsedTime(context);
        long twoDays = TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS);

        if (BRKeyStore.getFailCount(context) != 0 || (passTime + twoDays <= System.currentTimeMillis())) {
            useFingerPrint = false;
        }

        if (forceFingerprint) {
            useFingerPrint = true;
        }

        if (forcePin) {
            useFingerPrint = false;
        }

        final Activity app = (Activity) context;

        FragmentFingerprint fingerprintFragment;
        FragmentPin breadPin;

        if (keyguardManager.isKeyguardSecure()) {
            if (useFingerPrint) {
                fingerprintFragment = new FragmentFingerprint();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("message", message);
                fingerprintFragment.setArguments(args);
                fingerprintFragment.setCompletion(completion);
                FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                transaction.add(android.R.id.content, fingerprintFragment, FragmentFingerprint.class.getName());
                transaction.addToBackStack(null);
                if (!app.isDestroyed())
                    transaction.commitAllowingStateLoss();
            } else {
                breadPin = new FragmentPin();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("message", message);
                breadPin.setArguments(args);
                breadPin.setCompletion(completion);
                FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                transaction.add(android.R.id.content, breadPin, breadPin.getClass().getName());
                transaction.addToBackStack(null);
                if (!app.isDestroyed()) {
                    transaction.commitAllowingStateLoss();
                }
            }
        } else {
            sayNoScreenLock(app);
        }

    }

    private void sayNoScreenLock(final Activity app) {
        BRDialog.showCustomDialog(app,
                "",
                app.getString(R.string.Prompts_NoScreenLock_body_android),
                app.getString(R.string.AccessibilityLabels_close),
                null,
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        app.finish();
                    }
                }, null, new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        app.finish();
                    }
                }, 0);
    }

    public static boolean isFingerPrintAvailableAndSetup(Context context) {
        return Utils.isFingerprintAvailable(context) && Utils.isFingerprintEnrolled(context);
    }

    public interface OnPinSuccess {
        void onSuccess();
    }
}
