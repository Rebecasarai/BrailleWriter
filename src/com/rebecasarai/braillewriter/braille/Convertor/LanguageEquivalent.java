package com.rebecasarai.braillewriter.braille.Convertor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LanguageEquivalent {

    private Map<String,String> mAlphabet;
    private String mCharacteres;
    private String mBrailleChars;


    LanguageEquivalent(String lan) {

        mAlphabet = new LinkedHashMap<String, String>();
        iniatilizeAlphabet(lan);
    }

    /**
     * Fullfills the Braille Alphabet equivalent of the selected language that its passed as a
     * parameter.
     * @param lan String representing the selected language.
     */
    private void iniatilizeAlphabet(String lan){

        checkLanguage(lan);

        List<String> mapaLetras = Arrays.asList(mCharacteres.split(","));
        List<String> mapaBraille = Arrays.asList(mBrailleChars.split(","));

        Iterator<String> i1 = mapaLetras.iterator();
        Iterator<String> i2 = mapaBraille.iterator();

        while (i1.hasNext() && i2.hasNext()) {
            mAlphabet.put(i1.next(), i2.next());
        }
        if (i1.hasNext() || i2.hasNext()) notifyNotEquivalents();

    }

    /**
     * Checks and fullfills the Characters and the equivalent Braille characters accordingly of the selected
     * language
     * @param lan
     */
    private void checkLanguage(String lan) {
        switch (lan.toUpperCase()){
            case "ESPAÑOL":
                mCharacteres = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,1,2,3,4,5,6,7,8,9,0,ª,!,\",·,$,%,&,/,(,),=,?,¿,Ñ,¡,'";
                mBrailleChars = "⠁,⠃,⠉,⠙,⠑,⠋,⠛,⠓,⠊,⠚,⠅,⠇,⠍,⠝,⠕,⠏,⠟,⠗,⠎,⠞,⠥,⠧,⠺,⠭,⠽,⠵,⠼⠁,⠼⠃,⠼⠉,⠼⠙,⠼⠑,⠼⠋,⠼⠛,⠼⠓,⠼⠊,⠼⠚,⠦,⠖,⠠⠶,·,⠈⠎,⠨⠴,⠈⠯,⠸⠌,⠐⠣,⠐⠜,⠐⠶,⠦,⠘⠰⠦,⠝,⠦,⠄";

                break;

            case "ENGLISH":
                mCharacteres = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,1,2,3,4,5,6,7,8,9,0,ª,!,\",·,$,%,&,/,(,),=,?,'";
                mBrailleChars = "⠁,⠃,⠉,⠙,⠑,⠋,⠛,⠓,⠊,⠚,⠅,⠇,⠍,⠝,⠕,⠏,⠟,⠗,⠎,⠞,⠥,⠧,⠺,⠭,⠽,⠵,⠼⠁,⠼⠃,⠼⠉,⠼⠙,⠼⠑,⠼⠋,⠼⠛,⠼⠓,⠼⠊,⠼⠚,⠦,⠖,⠠⠶,·,⠈⠎,⠨⠴,⠈⠯,⠸⠌,⠐⠣,⠐⠜,⠐⠶,⠦,⠄";

                break;
            default:
                mCharacteres = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,1,2,3,4,5,6,7,8,9,0,ª,!,\",·,$,%,&,/,(,),=,?,'";
                mBrailleChars = "⠁,⠃,⠉,⠙,⠑,⠋,⠛,⠓,⠊,⠚,⠅,⠇,⠍,⠝,⠕,⠏,⠟,⠗,⠎,⠞,⠥,⠧,⠺,⠭,⠽,⠵,⠼⠁,⠼⠃,⠼⠉,⠼⠙,⠼⠑,⠼⠋,⠼⠛,⠼⠓,⠼⠊,⠼⠚,⠦,⠖,⠠⠶,·,⠈⠎,⠨⠴,⠈⠯,⠸⠌,⠐⠣,⠐⠜,⠐⠶,⠦,⠄";

                break;
        }
    }

    private void notifyNotEquivalents(){

    }


    public Map<String, String> getAlphabet() {
        return mAlphabet;
    }

}
