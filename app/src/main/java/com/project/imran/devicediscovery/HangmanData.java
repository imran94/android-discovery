package com.project.imran.devicediscovery;

import java.util.ArrayList;
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

    public HangmanData() {
        Random rand = new Random();

        wordIndex = rand.nextInt(4);
        imageIndex = 0;
        rightChars = new TreeSet<>();
        wrongChars = new TreeSet<>();
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
}
