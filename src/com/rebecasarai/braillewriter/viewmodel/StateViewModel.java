package com.rebecasarai.braillewriter.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.rebecasarai.braillewriter.fragments.ReadFragment;
import com.rebecasarai.braillewriter.subscription.Subscription;

public class StateViewModel extends AndroidViewModel {

    private MutableLiveData<Fragment> mSeletedFragment = new MutableLiveData<>();
    private MutableLiveData<TextToSpeech> tts = new MutableLiveData<>();
    private MutableLiveData<Subscription> mSubscription = new MutableLiveData<>();
    private MutableLiveData<Boolean> mSameFragment = new MutableLiveData<>();
    private boolean mFirstTime = true;

    public StateViewModel(@NonNull Application application) {
        super(application);

        mSeletedFragment.setValue(ReadFragment.getInstance());
        mSameFragment.setValue(false);
        mFirstTime = true;
    }

    public MutableLiveData<Boolean>getmSameFragment() {
        return mSameFragment;
    }


    public void setmSameFragment(Boolean mSameFragment) {
        this.mSameFragment.setValue(mSameFragment);
    }


    public MutableLiveData<Fragment> getmSeletedFragment() {
        if (mSeletedFragment == null) {
            mSeletedFragment.setValue(ReadFragment.getInstance());
        }
        return mSeletedFragment;
    }

    public void setmSeletedFragment(Fragment seletedFragment) {
        mFirstTime = false;
        mSameFragment.setValue(false);

        if (mSeletedFragment.getValue().getClass().getName().toUpperCase().equals(seletedFragment.getClass().getName().toUpperCase())) {
            mSameFragment.setValue(true);
        }

        this.mSeletedFragment.setValue(seletedFragment);
    }

    public boolean ismFirstTime() {
        return mFirstTime;
    }

    public void setmFirstTime(boolean mFirstTime) {
        this.mFirstTime = mFirstTime;
    }
}
