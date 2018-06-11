package com.rebecasarai.braillewriter;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.speech.tts.TextToSpeech;

import com.rebecasarai.braillewriter.subscription.Subscription;

import java.util.List;

public class MainViewModel extends ViewModel {
    private MutableLiveData<TextToSpeech> tts;
    private MutableLiveData<String> toSpeak;


    public LiveData<TextToSpeech> setTts() {
        if (tts == null) {
            tts = new MutableLiveData<TextToSpeech>();
            getTextToSpeech();
        }
        return tts;
    }

    private void getTextToSpeech() {
        // Do an asynchronous operation to fetch subscriptions.
    }


    public LiveData<TextToSpeech> setToSpeak() {
        if (toSpeak == null) {
            toSpeak = new MutableLiveData<String>();
            getToSpeak();
        }
        return tts;
    }

    private void getToSpeak() {
        // Do an asynchronous operation to fetch subscriptions.
    }
}