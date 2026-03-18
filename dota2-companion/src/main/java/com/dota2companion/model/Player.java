package com.dota2companion.model;

public class Player {
    private long accountId;
    private String personaName;
    private int mmrEstimate;
    private int rankTier;

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getPersonaName() {
        return personaName;
    }

    public void setPersonaName(String personaName) {
        this.personaName = personaName;
    }

    public int getMmrEstimate() {
        return mmrEstimate;
    }

    public void setMmrEstimate(int mmrEstimate) {
        this.mmrEstimate = mmrEstimate;
    }

    public int getRankTier() {
        return rankTier;
    }

    public void setRankTier(int rankTier) {
        this.rankTier = rankTier;
    }
}

