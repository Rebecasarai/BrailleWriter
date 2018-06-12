package com.rebecasarai.braillewriter.fragments;


import android.app.PendingIntent;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.rebecasarai.braillewriter.MainViewModel;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.subscription.Subscription;

import java.util.Locale;

import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 */
public class SubscribeFragment extends Fragment implements View.OnClickListener, TextToSpeech.OnInitListener {

    // Instance
    private static SubscribeFragment INSTANCE = new SubscribeFragment();
    private String toSpeak;
    private TextToSpeech tts;
    private View mLoadingView;
    private TextView mErrorTextView;
    //private SubscriptionManagerProvider mSubscriptionProvider;

    public TextView title, description;
    public Button button;
    MainViewModel model;

    public static SubscribeFragment getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SubscribeFragment();
        }
        return INSTANCE;
    }

    public SubscribeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_subscribe, container, false);
        findViews(root);

        setWaitScreen(true);
        //onManageReadyBilling((SubscriptionManagerProvider) getActivity());

        model = ViewModelProviders.of(getActivity()).get(MainViewModel.class);
        toSpeak = "Ha entrado a configuración";
        model.getIsSubscribed().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {

                toSpeak = "Prueba reconocer rostros y emociones gratis por 12 días";
                if (model.getmSameFragment().getValue() != null && model.getmSameFragment().getValue()) {
                    toSpeak = "";
                    model.getmSameFragment().setValue(false);
                }
                Subscription subsDetails = model.getMonthSubsDetails();
                showSubDetails(subsDetails);

            }
        });
        tts = new TextToSpeech(getContext(), this);

        return root;
    }

    private void findViews(View root) {
        mErrorTextView = (TextView) root.findViewById(R.id.error_textview_f);
        mLoadingView = root.findViewById(R.id.screen_wait);
        title = (TextView) root.findViewById(R.id.title_sub_f);
        description = (TextView) root.findViewById(R.id.description_sub_f);
        button = (Button) root.findViewById(R.id.state_button_sub_f);

    }

    /*public void onManageReadyBilling(SubscriptionManagerProvider billingProvider) {
        mSubscriptionProvider = billingProvider;
        toSpeak = "Prueba reconocer rostros y emociones gratis por 12 días";
       if( model.getmSameFragment().getValue()!= null && model.getmSameFragment().getValue()){
            toSpeak="";
            model.getmSameFragment().setValue(false);
       }
        Subscription subsDetails = mSubscriptionProvider.getSubsV3Manager().getMonthSubsDetails();
        showSubDetails(subsDetails);

    }*/

    public void showSubDetails(Subscription subsDetails) {
        button.setOnClickListener(this);
        title.setText(subsDetails.getTitle());
        description.setText(subsDetails.getDescription());
        setWaitScreen(false);
    }


    /**
     * Enables or disables "please wait" screen.
     */
    private void setWaitScreen(boolean set) {
        mLoadingView.setVisibility(set ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.state_button_sub_f:
                //mSubscriptionProvider.getSubsV3Manager().buySubscription();
                Bundle buyIntentBundle = model.buySubscription();
                PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                try {
                    getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.cuadro:
                //mSubscriptionProvider.getSubsV3Manager().buySubscription();
                Bundle buyIntentBundle2 = model.buySubscription();
                PendingIntent pendingIntent2 = buyIntentBundle2.getParcelable("BUY_INTENT");
                try {
                    getActivity().startIntentSenderForResult(pendingIntent2.getIntentSender(), 1001, new Intent(), 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Timber.e("This Language is not supported");
            } else {
                tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                toSpeak = "";
            }

        } else {
            Timber.e("Initilization Failed!");
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
}
