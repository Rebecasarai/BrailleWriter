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

import com.rebecasarai.braillewriter.BuildConfig;
import com.rebecasarai.braillewriter.MainViewModel;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.fragments.AboutFragment;
import com.rebecasarai.braillewriter.fragments.BrailleTranslatorFragment;
import com.rebecasarai.braillewriter.fragments.FacesFragment;
import com.rebecasarai.braillewriter.fragments.ObjectRecognitionFragment;
import com.rebecasarai.braillewriter.fragments.ReadFragment;
import com.rebecasarai.braillewriter.fragments.SubscribeFragment;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class HomeActivity extends AppCompatActivity {
    private Fragment selectedFragment;
    private boolean suscrito;
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
            suscrito = true;
        }

        model.getIsSubscribed().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null){
                    suscrito = aBoolean;
                }
            }
        });




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
                                if(model.checkSubscribedMonth()){
                                    selectedFragment = FacesFragment.newInstance();
                                }else{
                                    selectedFragment = SubscribeFragment.newInstance();
                                }
                                break;

                            case R.id.navigation_settings:
                                selectedFragment = AboutFragment.newInstance();
                                break;
                        }
                        model.setmSeletedFragment(selectedFragment);
                        return true;
                    }
                });

        model.getmSeletedFragment().observe(this, new Observer<Fragment>() {
            @Override
            public void onChanged(@Nullable Fragment fragment) {
                //selectedFragment = fragment;
                //Timber.e("ENTRO Selected fragment");

                if(fragment == selectedFragment){
                    Timber.e("igualessss");
                    model.getmSameFragment().setValue(true);
                }

                setFragment(fragment);
            }
        });


        if(selectedFragment == null){
            model.setmSeletedFragment(ReadFragment.newInstance());
        }




    }


    private void setFragment(Fragment selectedFragment){
        if(selectedFragment!=null){
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.framelayout, selectedFragment, "currentFragment");
            transaction.commit();
            model.getmSameFragment().setValue(false);
        }
    }


    @Override
    protected void onDestroy() {

        //subsV3Manager.doUnbindService();
        super.onDestroy();

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
        //if(subsV3Manager.checkSubscribedMonth() != initialStatus){
          //  setFragment(selectedFragment);
        //}

        if(model.checkSubscribedMonth() != suscrito){
          setFragment(selectedFragment);

        }
    }

}
