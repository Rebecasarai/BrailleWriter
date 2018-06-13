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

import com.rebecasarai.braillewriter.viewmodel.StateViewModel;
import com.rebecasarai.braillewriter.viewmodel.SubscriptionsMainViewModel;
import com.rebecasarai.braillewriter.fragments.ObjectRecognitionFragment;
import com.rebecasarai.braillewriter.fragments.SubscribeFragment;

import com.rebecasarai.braillewriter.BuildConfig;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.fragments.AboutFragment;
import com.rebecasarai.braillewriter.fragments.BrailleTranslatorFragment;
import com.rebecasarai.braillewriter.fragments.FacesFragment;
import com.rebecasarai.braillewriter.fragments.ReadFragment;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class HomeActivity extends AppCompatActivity {
    private Fragment mSelectedFragment;
    private SubscriptionsMainViewModel msubsVM;
    private StateViewModel mStateVM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }


        msubsVM = ViewModelProviders.of(this).get(SubscriptionsMainViewModel.class);
        mStateVM = ViewModelProviders.of(this).get(StateViewModel.class);


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
                                if(msubsVM.checkSubscribedMonth()){
                                    mSelectedFragment = FacesFragment.getInstance();
                                }else{
                                    mSelectedFragment = SubscribeFragment.getInstance();
                                }

                                break;

                            case R.id.navigation_settings:
                                mSelectedFragment = AboutFragment.getInstance();
                                break;
                        }
                        mStateVM.setmSeletedFragment(mSelectedFragment);

                        return true;
                    }
                });

        mStateVM.getmSeletedFragment().observe(this, new Observer<Fragment>() {
            @Override
            public void onChanged(@Nullable Fragment fragment) {
                setFragment(fragment);
            }
        });
    }


    /**
     * Sets the fragment selected to the Layout, so it shows it replaces the current one with the one passed as parameter
     * @param selectedFragment REpresenting the selected Fragment by the interaction with the menu
     */
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


                    msubsVM.setIsRecentlySuscribed(true);
                    mSelectedFragment = getSupportFragmentManager().findFragmentByTag("currentFragment");
                    if(mSelectedFragment != null){
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.framelayout, mSelectedFragment, "currentFragment");
                        transaction.commitAllowingStateLoss();
                        mStateVM.setmSeletedFragment(mSelectedFragment);
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
    }





}
