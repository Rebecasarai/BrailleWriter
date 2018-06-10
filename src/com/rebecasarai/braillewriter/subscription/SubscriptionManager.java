package com.rebecasarai.braillewriter.subscription;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import timber.log.Timber;


/**
 * This is the Subscriptions Manager, in here, all the methods related to In app Subscriptions are done.
 * From getting the Subscription Details to checking if a user is subscribed or not.
 * Using Version 3 of the In App Billing Google API, It was required to do several steps to make the
 * buying tests and later on the real ones.
 */
public class SubscriptionManager {

    private boolean mIsBinded;
    private Context context;
    private String tag;
    private IInAppBillingService mService;
    private Activity mActivity;
    private Gson gson = new Gson();
    private  boolean isRecentlySuscribed;

    /**
     * The Service Connection to being able to conect with the In App Android Billing API
     */
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };
    private final String subscriptionID =  "mothly_sub";	// The Google Play ID for BW subscriptions


    public SubscriptionManager(Activity activity) {
        this.context = activity.getApplicationContext();
        tag = "In App Billing";
        mActivity = activity;

        Intent billingIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        billingIntent.setPackage("com.android.vending");
        mIsBinded = context.bindService(billingIntent, mServiceConn, Context.BIND_AUTO_CREATE);

       Timber.i( "bindService - return " + String.valueOf(mIsBinded));
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
            ownedItems = mService.getPurchases(3, context.getPackageName(), "subs", null);

            Timber.i( "getPurchases() - success return Bundle");

        } catch (RemoteException e) {
            e.printStackTrace();
            Toast.makeText(context, "getPurchases - fail!", Toast.LENGTH_SHORT).show();
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
        Subscription skuObject = new Subscription();

        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(productSKUID);
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

        Bundle skuDetails;
        try {
            skuDetails = mService.getSkuDetails(3, context.getPackageName(), "subs", querySkus);
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
    public void buySubscription(){
        buySKU(subscriptionID);
    }

    /**
     * Buys a particular Product that this time is the monthly subscription, by making a request to the API
     * with a bundle Intent. This intent returns a code represented as an int taht is the result of the
     * operation. If the code returns a 0, means that the process went OK, and If so, it means that the user
     * either subscribed or cancelled the operation. The result of the buy operation is returned to the Home activity
     * at the onActivityOnResult
     * @param sku String Representing the Sku ID of the product to buy.
     */
    private void buySKU(String sku){
        if (!mIsBinded) return;
        if (mService == null) return;
        try{
            Bundle buyIntentBundle = mService.getBuyIntent(3, context.getPackageName(), sku, "subs", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
            int response = buyIntentBundle.getInt("RESPONSE_CODE");

            Timber.i(tag, "getBuyIntent() RESPONSE_CODE: " + String.valueOf(response));

            if (response != 0) notifyError();

            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            mActivity.startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);

            } catch (RemoteException e) {
                e.printStackTrace();

                Timber.w(tag, "getBuyIntent() failed");
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
        }
    }

    private void notifyError() {
    }


    public boolean isRecentlySuscribed() {
        return isRecentlySuscribed;
    }

    public void setRecentlySuscribed(boolean recentlySuscribed) {
        isRecentlySuscribed = recentlySuscribed;
    }

    /**
     * Clear the resources
     */
    public void destroy() {
        Timber.d("Destroying the manager.");

    }


}
