package com.rebecasarai.braillewriter.braille.Convertor;

import java.util.Map;

/**
 * Class that translates a document to Braille. Using a Map representing the Alphabet equivalent of Braille
 */
public class BrailleTranslator {


    private Map<String,String> mAlphabetMap;
    private String mLanguageSelected;

    BrailleTranslator(String lang) {

        mLanguageSelected = lang;

        setLanguageConfig(mLanguageSelected);
    }

    /**
     * Sets the language of document and so, the language to translate to braille with.
     * @param lan String representing the language
     */
    private void setLanguageConfig(String lan){

        LanguageEquivalent languageEquivalent = new LanguageEquivalent(lan);
        mAlphabetMap = languageEquivalent.getAlphabet();

    }

    /**
     * Translate a String data representing a line of the document received, to Braille unicode,
     * Using the Alphabet of the language equivalent. So, takes the String and makes it Upprcase
     * and loops in it, for each char Searchs it/ Maps it, in the mAphabetMap, getting the
     * correcponding Braille character.
     * @param data String representing the line of text to translate
     * @return String with the unicode braille character
     */
    public String translateToBraille(String data){
        String toTranslate = data.toUpperCase();
        String translated = "";

        for (char ch: toTranslate.toCharArray()) {
            for (Map.Entry<String, String> entry : mAlphabetMap.entrySet()) {

                if (entry.getKey().equals(String.valueOf(ch))) {

                    translated += ""+entry.getValue();
                }
            }
        }
        return translated;
    }

}
