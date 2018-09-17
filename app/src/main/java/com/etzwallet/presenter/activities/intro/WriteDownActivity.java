package com.etzwallet.presenter.activities.intro;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
//import android.widget.ImageButton;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.interfaces.BRAuthCompletion;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.security.AuthManager;
import com.etzwallet.tools.security.PostAuth;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;

import static java.security.AccessController.getContext;

public class WriteDownActivity extends BRActivity {
    private static final String TAG = WriteDownActivity.class.getName();
    private static WriteDownActivity app;
    private String storeAddress;
    public static WriteDownActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_down);

        confirmTheAddressIsTure();

        Button writeButton = findViewById(R.id.button_write_down);

        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                AuthManager.getInstance().authPrompt(WriteDownActivity.this, null, getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        PostAuth.getInstance().onPhraseCheckAuth(WriteDownActivity.this, false);
                    }

                    @Override
                    public void onCancel() {
    
                    }
                });

            }
        });
    }
    //判断地址是否正确
    private void confirmTheAddressIsTure(){
        final Context ctx = BreadApp.getBreadContext();
        final BaseWalletManager wm = WalletsMaster.getInstance(ctx).getCurrentWallet(ctx);
        storeAddress = wm.getAddress();
        Log.i(TAG, "run: storeAddress==="+storeAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
//            close();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    private void close() {
        BRAnimator.startBreadActivity(this, false);
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
        if (!isDestroyed()) {
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

}
