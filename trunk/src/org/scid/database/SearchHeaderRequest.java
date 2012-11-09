package org.scid.database;

public class SearchHeaderRequest {
  public String white, black, event, site,
    ecoFrom, ecoTo, yearFrom, yearTo, idFrom, idTo;
  public boolean ignoreColors,
    resultNone, resultWhiteWins, resultBlackWins, resultDraw,
    ecoNone;
}
