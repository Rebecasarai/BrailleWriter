package com.rebecasarai.braillewriter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.rebecasarai.braillewriter.fragments.SubscribeFragment;

import com.rebecasarai.braillewriter.BuildConfig;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.fragments.AboutFragment;
import com.rebecasarai.braillewriter.fragments.BrailleTranslatorFragment;
import com.rebecasarai.braillewriter.fragments.FacesFragment;
import com.rebecasarai.braillewriter.fragments.ReadFragment;
import com.rebecasarai.braillewriter.fragments.ObjectRecognitionFragment;
import com.rebecasarai.braillewriter.subscription.SubscriptionManager;
import com.rebecasarai.braillewriter.subscription.SubscriptionManagerProvider;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class HomeActivity extends AppCompatActivity implements SubscriptionManagerProvider {
    private SubscriptionManager subsV3Manager;
    private Fragment selectedFragment;
    private boolean initialStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.navigation);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        subsV3Manager = new SubscriptionManager(this);
        initialStatus = subsV3Manager.checkSubscribedMonth();

        bottomNavigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        selectedFragment = null;
                        switch (item.getItemId()) {
                            case R.id.navigation_read:
                                selectedFragment = ReadFragment.newInstance();
                                break;
                            case R.id.navigation_recognite:
                                selectedFragment = ObjectRecognitionFragment.newInstance();

                                break;
                            case R.id.navigation_translate:
                                selectedFragment = BrailleTranslatorFragment.newInstance();
                                break;

                            case R.id.navigation_faces:
                                if(subsV3Manager.checkSubscribedMonth()){
                                    selectedFragment = FacesFragment.newInstance();
                                }else{
                                    selectedFragment = SubscribeFragment.newInstance();
                                }

                                break;

                            case R.id.navigation_settings:
                                selectedFragment = AboutFragment.newInstance();
                                break;
                        }
                        setFragment(selectedFragment);

                        return true;
                    }
                });
        setFragment(ReadFragment.newInstance());
    }


    private void setFragment(Fragment selectedFragment){
        if(selectedFragment!=null){
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.framelayout, selectedFragment, "currentFragment");
            transaction.commit();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        subsV3Manager.destroy();
    }

    @Override
    public SubscriptionManager getSubsV3Manager() {
        return subsV3Manager;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Timber.e("You have bought the " + sku + ". Excellent choice, adventurer!");

                    subsV3Manager.setRecentlySuscribed(true);
                    selectedFragment = getSupportFragmentManager().findFragmentByTag("currentFragment");
                    if(selectedFragment != null){
                        //setFragment(selectedFragment);
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.framelayout, selectedFragment, "currentFragment");
                        transaction.commitAllowingStateLoss();
                    }
                }
                catch (JSONException e) {
                    Timber.e("Failed to parse purchase data.");
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(subsV3Manager.checkSubscribedMonth() != initialStatus){
            setFragment(selectedFragment);
        }
    }
}
