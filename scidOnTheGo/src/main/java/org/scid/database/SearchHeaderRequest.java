package org.scid.database;

public class SearchHeaderRequest {
    public String white, black, event, site, round,
        ecoFrom, ecoTo;
    public boolean ignoreColors,
        whiteExact, blackExact, eventExact, siteExact, roundExact,
        resultNone, resultWhiteWins, resultBlackWins, resultDraw,
        halfMovesEven, halfMovesOdd,
        allowEcoNone, allowUnknownElo, annotatedOnly;
    public int dateMin, dateMax, idMin, idMax, halfMovesMin, halfMovesMax,
        whiteEloMin, whiteEloMax, blackEloMin, blackEloMax,
        diffEloMin, diffEloMax,
        minEloMin, minEloMax, maxEloMin, maxEloMax;

    // from date.h
    private static final int YEAR_SHIFT = 9;
    private static final int MONTH_SHIFT = 5;
    public static final int YEAR_MAX = 2047;
    public static int makeDate(int y, int m, int d) {
        return (y == 0) ? 0 : (y << YEAR_SHIFT) | (m << MONTH_SHIFT) | d;
    }

    // from index.h
    public static final int MAX_ELO = 4000;
}
