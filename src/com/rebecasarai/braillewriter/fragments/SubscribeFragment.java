package com.rebecasarai.braillewriter.fragments;


import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.TextView;

import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.subscription.Subscription;
import com.rebecasarai.braillewriter.subscription.SubscriptionManagerProvider;

import java.util.Locale;

import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 */
public class SubscribeFragment extends Fragment implements View.OnClickListener, TextToSpeech.OnInitListener {

    private String toSpeak;
    private TextToSpeech tts;
    private View mLoadingView;
    private TextView mErrorTextView;
    private SubscriptionManagerProvider mSubscriptionProvider;

    public TextView title, description;
    public Button button;

    public static SubscribeFragment newInstance() {
        SubscribeFragment fragment = new SubscribeFragment();
        return fragment;
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
        onManageReadyBilling((SubscriptionManagerProvider) getActivity());

        tts = new TextToSpeech(getContext(), this);

        return root;
    }

    private void findViews(View root){
        mErrorTextView = (TextView) root.findViewById(R.id.error_textview_f);
        mLoadingView = root.findViewById(R.id.screen_wait);
        title = (TextView) root.findViewById(R.id.title_sub_f);
        description = (TextView) root.findViewById(R.id.description_sub_f);
        button = (Button) root.findViewById(R.id.state_button_sub_f);

    }

    public void onManageReadyBilling(SubscriptionManagerProvider billingProvider) {
        mSubscriptionProvider = billingProvider;
        toSpeak = "Prueba reconocer rostros y emociones gratis por 12 días";
        Subscription subsDetails = mSubscriptionProvider.getSubsV3Manager().getMonthSubsDetails();
        showSubDetails(subsDetails);

    }

    public void showSubDetails(Subscription subsDetails){
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
        switch (v.getId()){

            case R.id.state_button_sub_f:
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
}
