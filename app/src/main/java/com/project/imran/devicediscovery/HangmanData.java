package com.project.imran.devicediscovery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Administrator on 01-Sep-16.
 */
public class HangmanData extends Object {

    public int imageIndex;
    public int wordIndex;
    public SortedSet<Character> rightChars;
    public SortedSet<Character> wrongChars;
    public char mostRecent;
    Random rand = new Random();

    public HangmanData() {
        wordIndex = rand.nextInt(4);
        imageIndex = 0;
        rightChars = new TreeSet<>();
        wrongChars = new TreeSet<>();
        mostRecent = ' ';
    }

    public HangmanData(int imageIndex,
                       int wordIndex,
                       SortedSet<Character> keyword,
                       SortedSet<Character> wrongChars) {
        this.imageIndex = imageIndex;
        this.wordIndex = wordIndex;
        this.rightChars = keyword;
        this.wrongChars = wrongChars;
    }

    public void restart() {
        wordIndex = rand.nextInt(4);
        imageIndex = 0;
        rightChars.clear();
        wrongChars.clear();
        mostRecent = ' ';
    }

    @Override
    public String toString() {
        String s = "0";
        s+= Integer.toString(imageIndex);
        s+= Integer.toString(wordIndex);

        Iterator<Character> iterator = wrongChars.iterator();
        while (iterator.hasNext()) {
            s += iterator.next();
        }

        s+= " ";

        iterator = rightChars.iterator();
        while(iterator.hasNext()) {
            s += iterator.next();
        }
        s += "/";

        s += mostRecent;

        return s;
    }

    public byte[] serialize() {
        return toString().getBytes();
    }

    public void deserialize(String s) {
        int i = 0;
        imageIndex = (Character.getNumericValue(s.charAt(++i)));
        wordIndex = (Character.getNumericValue(s.charAt(++i)));

        wrongChars.clear();
        while (s.charAt(++i) != ' ') {
            wrongChars.add(s.charAt(i));
        }

        rightChars.clear();
        while(s.charAt(++i) != '/') {
            rightChars.add(s.charAt(i));
        }

        mostRecent = s.charAt(++i);
    }
}
