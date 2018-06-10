package com.rebecasarai.braillewriter.braille.Convertor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

/**
 * Class that manages all the files processes to translate Spanish or English to Braille.
 * Is called by the BrailleTranslatorFragment to Read new documents and then call th BrailleTranslator
 * to go line by line, then character by character and return the BRaille translation data to be saved
 * in a new file on the device storage.
 */
public class FileManagerConvertor {

    private Context mContext;

    public FileManagerConvertor(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Reads the text of a document that can be either English or Spanish and must be specified on the params, along
     * with its Uri. Creates a Braille Translator, passing the selected langue to the constructor and with a Buffered
     * Reader, opens the selected file with the android Storage Framework.
     * Validates that the inputStream is not null. Then creates a String representing each lien of the file and passes
     * it to the Braille Translator.
     * @param uriFile Representing the Uri of the file
     * @param language The Selected language
     * @return String representing the text translated to Braille.
     */
    public String readTextFromUri(Uri uriFile, String language) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            BrailleTranslator brailleTranslator = new BrailleTranslator(language);

            InputStream inputStream = mContext.getContentResolver().openInputStream(uriFile);

            BufferedReader reader = null;
            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(
                        inputStream));
            }

            String line;

            if (reader != null) {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(brailleTranslator.translateToBraille(line));

                }
            }
        }catch (Exception e){
            stringBuilder.append("");
        }

        return stringBuilder.toString();
    }


    /**
     * Writes the Braille translated unicode characters to a file and saves it on the device internal
     * storage, allowing also, to save it directly on Google Documents from Drive.
     * @param data String representing the Translated Braille
     */
    public void writeToFile(String data) {
        try {

            File root = new File(Environment.getExternalStorageDirectory(), "Braille");
            if (!root.exists()) {
                root.mkdirs();
            }
            Date firstDate = new Date();
            String date = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault()).format(firstDate);
            File gpxfile = new File(root, "braille"+date+".txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(data);
            writer.flush();
            writer.close();
            Toast.makeText(mContext, "Guardado"+ gpxfile.getName(), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(gpxfile), "text/plain");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);

        }
        catch (IOException e) {
            Timber.e( "File write failed: " + e.toString());
        }
    }



}
