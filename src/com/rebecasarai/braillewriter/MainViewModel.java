package com.rebecasarai.braillewriter;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.firebase.database.MutableData;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.rebecasarai.braillewriter.fragments.ReadFragment;
import com.rebecasarai.braillewriter.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MainViewModel extends AndroidViewModel {


    private Repository mRepository;
    private MutableLiveData<Fragment> mSeletedFragment = new MutableLiveData<>();
    private MutableLiveData<TextToSpeech> tts = new MutableLiveData<>();
    private MutableLiveData<Subscription> mSubscription = new MutableLiveData<>();
    private MutableLiveData<Boolean> mSameFragment = new MutableLiveData<>();
    private MutableLiveData<ServiceConnection> mServiceConn = new MutableLiveData<>();


    private boolean mIsBinded;
    private String tag;
    Application app;
    private IInAppBillingService mService;
    private Gson gson = new Gson();
    private MutableLiveData<Boolean> isRecentlySuscribed = new MutableLiveData<>();
    private final String subscriptionID =  "mothly_sub";	// The Google Play ID for BW subscriptions

    private MutableLiveData<Boolean> isSubscribed = new MutableLiveData<>();





    public MainViewModel(@NonNull Application application) {
        super(application);
        createService();
        app = application;
        //context = application.getApplicationContext();
        tag = "In App Billing";
        isRecentlySuscribed.setValue(false);

        Intent billingIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        billingIntent.setPackage("com.android.vending");
        mIsBinded = app.bindService(billingIntent, mServiceConn.getValue(), Context.BIND_AUTO_CREATE);

        isSubscribed.setValue(checkSubscribedMonth());
        mSeletedFragment.setValue(ReadFragment.newInstance());
        Timber.i( "bindService - return " + String.valueOf(mIsBinded));
    }


    public MutableLiveData<Boolean> getIsSubscribed() {
        if(isSubscribed == null){
            isSubscribed.setValue(false);
        }
        isSubscribed.setValue(checkSubscribedMonth());
        return isSubscribed;
    }


    /**
     * The Service Connection to being able to conect with the In App Android Billing API
     */
    public void createService(){
        mServiceConn.setValue(new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);
            }
        });

    }






    /**
     * Checks if the current user is subscribed to the BW monthly subscription
     * It
     * @return
     */
    public boolean checkSubscribedMonth(){
        boolean isSubscribed = false;
        if (!mIsBinded) return false;
        if (mService == null) return false;

        Bundle ownedItems;
        try {
            ownedItems = mService.getPurchases(3, app.getPackageName(), "subs", null);

            Timber.i( "getPurchases() - success return Bundle");

        } catch (RemoteException e) {
            e.printStackTrace();
            Toast.makeText(app, "getPurchases - fail!", Toast.LENGTH_SHORT).show();
            Timber.w(tag, "getPurchases() - fail!");
            return false;
        }

        int response = ownedItems.getInt("RESPONSE_CODE");
        Timber.i( "getPurchases() - \"RESPONSE_CODE\" return " + String.valueOf(response));

        if (response != 0) return false;

        ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
        ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
        ArrayList<String> signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");
        String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

        Timber.i(tag, "getPurchases() - \"INAPP_PURCHASE_ITEM_LIST\" return " + ownedSkus.toString());
        Timber.i( "getPurchases() - \"INAPP_PURCHASE_DATA_LIST\" return " + purchaseDataList.toString());
        Timber.i( "getPurchases() - \"INAPP_DATA_SIGNATURE\" return " + (signatureList != null ? signatureList.toString() : "null"));
        Timber.i( "getPurchases() - \"INAPP_CONTINUATION_TOKEN\" return " + (continuationToken != null ? continuationToken : "null"));

        if(ownedSkus.contains(subscriptionID) ){
            isSubscribed = true;
        }
        return isSubscribed;
    }

    /**
     * Gets the Details of a particular product, making a request to the API with all products and getting the one with
     * the product ID. This request returns a Json Object with all the information about that subscription product,
     * which I parsed to a Subscription Object.
     * @param productSKUID String representing the ID name of a product, in this case the monthly sub.
     * @return Subscription Object representing the product requested to the API.
     */
    private Subscription getSKUDetails(String productSKUID){
        if (!mIsBinded) return null;
        if (mService == null) return null;
        Subscription skuObject = new Subscription("","","","","");

        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(productSKUID);
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

        Bundle skuDetails;
        try {
            skuDetails = mService.getSkuDetails(3, app.getPackageName(), "subs", querySkus);
        } catch (RemoteException e) {
            e.printStackTrace();
            Timber.e(tag, "getSkuDetails() failed");
            return skuObject;
        }

        int response = skuDetails.getInt("RESPONSE_CODE");
        Timber.i( "getSkuDetails() - \"RESPONSE_CODE\" return " + String.valueOf(response));

        if (response != 0) return skuObject;

        ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
        Timber.i( "getSkuDetails() - \"DETAILS_LIST\" return " + responseList.toString());

        if (responseList.size() == 0) return skuObject;

        for (String thisResponse : responseList) {

            try {
                skuObject = gson.fromJson(thisResponse, Subscription.class);

                if (!skuObject.getProductId().equals(subscriptionID)) continue;

            } catch (JsonParseException e) {
                e.printStackTrace();
                Timber.e("Gson parse failed: "+e);
            }
        }

        return skuObject;
    }

    /**
     * Gets the Details of the subscription product. This allows to show on fragments the information
     * related to the subscription. Calls getSKUDetails, that returns a JSON Object.
     * @return Subscription Object representing the product once parsed from the JSON object.
     */
    public Subscription getMonthSubsDetails(){
        return getSKUDetails(subscriptionID);
    }

    /**
     * To suscribe from any Fragment, and using the Subscription Manager Provider Interfaz in the Activity,
     * This is called when the user press the button to suscribe being either on Faces Fragment or About
     * Pases the current ProductID to buySKU().
     */
    public Bundle buySubscription(){
        return buySKU(subscriptionID);
    }

    /**
     * Buys a particular Product that this time is the monthly subscription, by making a request to the API
     * with a bundle Intent. This intent returns a code represented as an int taht is the result of the
     * operation. If the code returns a 0, means that the process went OK, and If so, it means that the user
     * either subscribed or cancelled the operation. The result of the buy operation is returned to the Home activity
     * at the onActivityOnResult
     * @param sku String Representing the Sku ID of the product to buy.
     */
    private Bundle buySKU(String sku){

        if (!mIsBinded) return null;
        if (mService == null) return null;
        Bundle buyIntentBundle = null;
        try{
            buyIntentBundle = mService.getBuyIntent(3, app.getPackageName(), sku, "subs", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
            int response = buyIntentBundle.getInt("RESPONSE_CODE");

            Timber.i(tag, "getBuyIntent() RESPONSE_CODE: " + String.valueOf(response));

            if (response != 0) notifyError();
        } catch (RemoteException e) {
            e.printStackTrace();

            Timber.w(tag, "getBuyIntent() failed");
        }
        return buyIntentBundle;
    }

    private void notifyError() {
    }



    public MutableLiveData<Boolean> getIsRecentlySuscribed() {
        if(isRecentlySuscribed == null){
            isRecentlySuscribed.setValue(false);
        }
        return isRecentlySuscribed;
    }

    public void setIsRecentlySuscribed(Boolean isRecentlySuscribed) {
        this.isRecentlySuscribed.setValue(isRecentlySuscribed);
    }











    public MutableLiveData<Boolean> getmSameFragment() {
        if(mSameFragment == null){
            mSameFragment.setValue(false);
        }
        return mSameFragment;
    }



    public void setmSameFragment(Boolean mSameFragment) {
        this.mSameFragment.setValue(mSameFragment);
    }


    public MutableLiveData<Fragment> getmSeletedFragment() {
        if(mSeletedFragment == null){
            mSeletedFragment.setValue(ReadFragment.newInstance());
        }
        return mSeletedFragment;
    }

    public void setmSeletedFragment(Fragment seletedFragment) {
        mSameFragment.setValue(false);

        if(mSeletedFragment.getValue() == seletedFragment){
            mSameFragment.setValue(true);
        }

        this.mSeletedFragment.setValue(seletedFragment);
    }


}