package com.rebecasarai.braillewriter.fragments;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.rebecasarai.braillewriter.viewmodel.StateViewModel;
import com.rebecasarai.braillewriter.viewmodel.SubscriptionsMainViewModel;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.braille.Convertor.FileManagerConvertor;

import java.util.Locale;

import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class BrailleTranslatorFragment extends Fragment implements View.OnClickListener, TextToSpeech.OnInitListener{


    private static final int EDIT_REQUEST_CODE = 44;
    // Unique request code.

    // Instance
    private static BrailleTranslatorFragment INSTANCE = new BrailleTranslatorFragment();

    Spinner spinner;
    private String TAG;
    private Uri toTranslate;
    private TextToSpeech tts;
    private String toSpeak;


    public BrailleTranslatorFragment() {
    }


    public static BrailleTranslatorFragment getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BrailleTranslatorFragment();
        }
        return INSTANCE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_convertor, container, false);


        spinner = (Spinner) root.findViewById(R.id.languagesSpinnerId);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.languages_string_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        final Button mConvertFileButton = (Button) root.findViewById(R.id.convertFileButton);
        final TextView test = (TextView) root.findViewById(R.id.test);
        mConvertFileButton.setOnClickListener(this);
        TAG = root.getClass().getName();

        StateViewModel model = ViewModelProviders.of(getActivity()).get(StateViewModel.class);
        toSpeak = "Ha entrado a traductor a braille";


        if(model.getmSameFragment().getValue()){
            toSpeak ="";
        }

        tts = new TextToSpeech(getContext(), this);


        return root;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        //if (requestCode == EDIT_REQUEST_CODE ) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (resultData != null) {
                Timber.e("Llega aqui a uri resultData no nulo");
                startTranlationToBraille(resultData.getData());

            }
        //}
    }

    /**
     *
     * @param uri
     */
    private void startTranlationToBraille(Uri uri){

        Timber.i("Uri: %s", uri.toString());
        String languageSelected = spinner.getSelectedItem().toString();
        FileManagerConvertor fileManagerConvertor = new FileManagerConvertor(getContext());
        String translated = fileManagerConvertor.readTextFromUri(uri, languageSelected);
        if (!translated.equals("")) {
            fileManagerConvertor.writeToFile(translated);
            toSpeak = "Traducido correctamente";

        } else {
            toSpeak = "Lo sentimos, no se ha traducido correctamente";
        }
        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

    }


    @Override
    public void onClick(View v) {
        openDocument();
        switch (v.getId()) {
            case R.id.convertFileButton:



                break;

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

    @Override
    public void onStop() {
        if (tts != null) {
            tts.stop();
        }
        super.onStop();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Timber.e("This Language is not supported");
            } else {
                tts.speak("Ha entrado a traductor a braille", TextToSpeech.QUEUE_FLUSH, null);
                toSpeak = "";
            }

        } else {
            Timber.e("Initilization Failed!");
        }
    }


    /**
     * Open a file for writing and append some text to it.
     */
    private void openDocument() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's
        // file browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only text files.
        intent.setType("text/plain");

        getInstance().startActivityForResult(intent, EDIT_REQUEST_CODE);

    }
}
