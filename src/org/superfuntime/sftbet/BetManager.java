package org.superfuntime.sftbet;

import java.util.*;

/**
 * This class manages the bets and the people betting for the matches, also instantiable to put in a queue
 * Created by Kosmic on 12/26/2016.
 */
public class BetManager {
    // use decimals for %
    private static double winnerPercent = 0.05; // auto percentage of pot that winner fighter takes
    private int state; // 0-Betting 1-fighting >1-done
    private Map<String, Map<String, Double>> betInfo; // all the other people

    public BetManager(Collection<String> fighters){
        state = 0;
        betInfo = new HashMap();
        for(String ftr : fighters){
            betInfo.put(ftr, new HashMap()); // init people to bet for
        }
    }

    public boolean addBet(String fighter, String name, double amount){
        Map bets = betInfo.get(fighter);
        if(bets != null && state == 0){
            if(bets.containsKey(name)) {
                bets.replace(name, amount);
            }else{
                bets.put(name, amount);
            }
            return true;
        }
        return false;
    }

    public static void setWinnerPercent(double perc){ // not 5 but 0.05 to do 5%
        winnerPercent = perc;
    }

    public static double getWinnerPercent(){ return winnerPercent; }

    public void setState(int s){ state = s; }

    public int getState(){ return state; }

    public Set<String> getFighters(){ return betInfo.keySet();}

    public Map<String, Double> calcWins(String winner){
        if(state >= 2) {
            // first part gathers all the money from the losers
            double loseAmount = 0.0;
            for (String loser : betInfo.keySet()) {
                if (!loser.equals(winner)) {
                    for (Double money : betInfo.get(loser).values()) {
                        loseAmount += money;
                    }
                }
            }

            // second part distributes money based on bet amount (plus default fighter win amount)
            double wins = loseAmount * winnerPercent; // money that fighter takes
            loseAmount -= wins;

            double winBet = 0.0;
            for (Double money : betInfo.get(winner).values()) {
                winBet += money;
            }

            Map<String, Double> gains = new HashMap();

            // calculates the percent of pot that goes to each bet winner
            if (winBet != 0) {
                for (String wnr : betInfo.get(winner).keySet()) {
                    int mg = ((int) (100 * (loseAmount * (betInfo.get(winner).get(wnr) / winBet))));
                    if (mg != 0) {
                        gains.put(wnr, mg * 0.01);
                    }
                }
            }

            // checks to see whether all the money should go to fighter due to lack of bet winners (other than fighter)
            if (gains.isEmpty()) {
                gains.put(winner, loseAmount);
            }

            if (gains.containsKey(winner)) { // makes sure to add fighter's winnings to total
                gains.replace(winner, ((int) (100 * (gains.get(winner) + wins))) * 0.01);
            } else {
                gains.put(winner, ((int) (100 * wins)) * 0.01);
            }

            return gains;
        }

        return null;
    }

    @Override
    public String toString(){
        String s = "";
        for(String fitr : betInfo.keySet()){
            String fn = (fitr + " |------------------------------").substring(0, 32); // right padding for 32 chars
            s += fn + "\n";
            for(String btr : betInfo.get(fitr).keySet()){
                String pn = (btr + "                ").substring(0, 16); // right padding for 16 chars
                s += "    " + pn + " " + betInfo.get(fitr).get(btr) + "\n";
            }
        }
        return s;
    }
}
