package com.dota2companion.model;

public class HeroStats {
    private int heroId;
    private String localizedName;
    private double winRate;
    private double gpm;
    private double xpm;
    private double globalWinRate;
    private double globalGpm;

    public int getHeroId() {
        return heroId;
    }

    public void setHeroId(int heroId) {
        this.heroId = heroId;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public double getGpm() {
        return gpm;
    }

    public void setGpm(double gpm) {
        this.gpm = gpm;
    }

    public double getXpm() {
        return xpm;
    }

    public void setXpm(double xpm) {
        this.xpm = xpm;
    }

    public double getGlobalWinRate() {
        return globalWinRate;
    }

    public void setGlobalWinRate(double globalWinRate) {
        this.globalWinRate = globalWinRate;
    }

    public double getGlobalGpm() {
        return globalGpm;
    }

    public void setGlobalGpm(double globalGpm) {
        this.globalGpm = globalGpm;
    }
}

