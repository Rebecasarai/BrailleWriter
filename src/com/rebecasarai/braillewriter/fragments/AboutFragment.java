package com.rebecasarai.braillewriter.fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.subscription.Subscription;
import com.rebecasarai.braillewriter.subscription.SubscriptionManagerProvider;

import java.util.Locale;

import timber.log.Timber;


/**
 * A {@link Fragment} subclass. That sets the About and subscription part of the app.
 */
public class AboutFragment extends Fragment implements View.OnClickListener, TextToSpeech.OnInitListener {

    private String toSpeak;
    private TextToSpeech tts;
    private View mLoadingView;
    private TextView mErrorTextView;
    private SubscriptionManagerProvider mSubscriptionProvider;
    private TextView title, description, price;
    private Button button;
    LinearLayout cuadro;

    public AboutFragment() {
        // Required empty public constructor
    }

    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_about, container, false);
        findViews(root);

        setWaitScreen(true);
        onManageReadyBilling((SubscriptionManagerProvider) getActivity());

        tts = new TextToSpeech(getContext(), this);

        return root;
    }


    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        checkIfChangedStatus();
    }

    /**
     * Checks if it has been a change in the currtent state of the user, meaning, if it has subscribed
     * or unsubscribed during the time of being in the fragment.
     */
    private void checkIfChangedStatus() {
        if(mSubscriptionProvider.getSubsV3Manager().checkSubscribedMonth()){
            displayAccordingly();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.boxWhenSuscribed:
                Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.rebecasarai.braillewriter");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;

            case R.id.state_button_sub:
                mSubscriptionProvider.getSubsV3Manager().buySubscription();
                break;

            case R.id.cuadro:
                mSubscriptionProvider.getSubsV3Manager().buySubscription();
                break;
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Timber.e( "This Language is not supported");
            } else {
                tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                toSpeak = "";
            }

        } else {
            Timber.e( "Initilization Failed!");
        }

    }

    @Override
    public void onDestroy() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Sets the Layout views
     * @param root Representing the layout view
     */
    private void findViews(View root){

        TextView myClickableUrl = (TextView) root.findViewById(R.id.link);
        myClickableUrl.setText("Para conocer más puede entrar en: \n http://braillewriter.es/politica-de-privacidad/");
        Linkify.addLinks(myClickableUrl, Linkify.WEB_URLS);

        mErrorTextView = (TextView) root.findViewById(R.id.error_textview);
        mLoadingView = root.findViewById(R.id.screen_wait);
        cuadro = (LinearLayout) root.findViewById(R.id.cuadro);
        title = (TextView) root.findViewById(R.id.title_sub);
        price = (TextView) root.findViewById(R.id.price_sub);
        description = (TextView) root.findViewById(R.id.description_sub);
        button = (Button) root.findViewById(R.id.state_button_sub);
    }

    /**
     * Sets the Subscriptions Manager Interface and sets the String to speak to
     * the regular one to guide the user.
     * @param billingProvider
     */
    public void onManageReadyBilling(SubscriptionManagerProvider billingProvider) {
        mSubscriptionProvider = billingProvider;
        toSpeak = "Ha entrado a configuración";
        displayAccordingly();
    }

    /**
     * Determines with the SubscriptionManager whereas the user its subscribed or not.
     * If its recently subscribed, meaning its the first fragment its getting into after subscribing,
     * the string to speak is to let him/her know its successfully subscribed.
     */
    private void displayAccordingly(){
        if(mSubscriptionProvider.getSubsV3Manager().checkSubscribedMonth()){
            if(mSubscriptionProvider.getSubsV3Manager().isRecentlySuscribed()){
                toSpeak = "Felicidades, se ha suscrito exitosamente.";
                mSubscriptionProvider.getSubsV3Manager().setRecentlySuscribed(false);
            }
            showManageSubs();

        }else{
            Subscription subsDetails = mSubscriptionProvider.getSubsV3Manager().getMonthSubsDetails();
            showSubDetails(subsDetails);
        }
    }

    /**
     * Shows the active and possible subscription details
     * @param subsDetails subscription Object representing the
     */
    public void showSubDetails(Subscription subsDetails){
        button.setOnClickListener(this);
        cuadro.setOnClickListener(this);
        title.setText(subsDetails.getTitle());
        price.setText(subsDetails.getPrice());
        description.setText(subsDetails.getDescription());
        setWaitScreen(false);
    }

    /**
     * Sets the views if the user is already subscribed
     */
    public void showManageSubs(){
        button.setVisibility(View.GONE);
        description.setText(R.string.manage_sub);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        cuadro.setId(R.id.boxWhenSuscribed);
        setWaitScreen(false);
    }


    /**
     * Enables or disables "please wait" screen.
     */
    private void setWaitScreen(boolean set) {
        mLoadingView.setVisibility(set ? View.VISIBLE : View.GONE);
    }


    private void displayAnErrorIfNeeded() {
        if (getActivity() == null || getActivity().isFinishing()) {
            Timber.e("No need to show an error - activity is finishing already");
            return;
        }

        mLoadingView.setVisibility(View.GONE);
        mErrorTextView.setVisibility(View.VISIBLE);
        mErrorTextView.setText("error");
    }

}



