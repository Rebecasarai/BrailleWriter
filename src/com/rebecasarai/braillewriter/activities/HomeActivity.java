package com.rebecasarai.braillewriter.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.rebecasarai.braillewriter.MainViewModel;
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

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class HomeActivity extends AppCompatActivity implements SubscriptionManagerProvider {
    private SubscriptionManager subsV3Manager;
    private Fragment mSelectedFragment;
    private boolean initialStatus;
    private boolean isSubscribed;
     MainViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.navigation);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        //subsV3Manager = new SubscriptionManager(this,getApplication());
        if(savedInstanceState == null){
            //subsV3Manager.createService();
        }

        //subsV3Manager.createService();
        //initialStatus = subsV3Manager.checkSubscribedMonth();


        model = ViewModelProviders.of(this).get(MainViewModel.class);

        if(model.getIsSubscribed().getValue()){
            isSubscribed = true;
        }

        model.getIsSubscribed().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null){
                    isSubscribed = aBoolean;
                }
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        mSelectedFragment = null;
                        switch (item.getItemId()) {
                            case R.id.navigation_read:
                                mSelectedFragment = ReadFragment.getInstance();
                                break;
                            case R.id.navigation_recognite:
                                mSelectedFragment = ObjectRecognitionFragment.getInstance();

                                break;
                            case R.id.navigation_translate:
                                mSelectedFragment = BrailleTranslatorFragment.getInstance();
                                break;

                            case R.id.navigation_faces:
                                //if(subsV3Manager.checkSubscribedMonth()){
                                //if(model.checkSubscribedMonth()){
                                if(model.checkSubscribedMonth()){
                                    mSelectedFragment = FacesFragment.getInstance();
                                }else{
                                    mSelectedFragment = SubscribeFragment.getInstance();
                                }

                                //}else{

                                //}

                                //}else{
                                //}

                                break;

                            case R.id.navigation_settings:
                                mSelectedFragment = AboutFragment.getInstance();
                                break;
                        }
                        model.setmSeletedFragment(mSelectedFragment);
                        //setFragment(mSelectedFragment);

                        return true;
                    }
                });


        /*if(mSelectedFragment == null){
            model.setmSeletedFragment(ReadFragment.newInstance());
        }*/

        model.getmSeletedFragment().observe(this, new Observer<Fragment>() {
            @Override
            public void onChanged(@Nullable Fragment fragment) {
                //mSelectedFragment = fragment;
                //Timber.e("ENTRO Selected fragment");

                setFragment(fragment);
            }
        });


        model.getmSameFragment().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                Timber.e("ENTRO SAME FRAGMENT");
            }
        });



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

        //subsV3Manager.doUnbindService();
        super.onDestroy();

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


                    model.setIsRecentlySuscribed(true);

                    //subsV3Manager.setRecentlySuscribed(true);
                    mSelectedFragment = getSupportFragmentManager().findFragmentByTag("currentFragment");
                    if(mSelectedFragment != null){
                        //setFragment(mSelectedFragment);
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.framelayout, mSelectedFragment, "currentFragment");
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
        //if(subsV3Manager.checkSubscribedMonth() != initialStatus){
          //  setFragment(mSelectedFragment);
        //}

        if(model.checkSubscribedMonth() != isSubscribed){
          setFragment(mSelectedFragment);

        }
    }





}
