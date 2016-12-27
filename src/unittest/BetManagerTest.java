package unittest;

import org.superfuntime.sftbet.BetManager;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by Kosmic on 12/26/2016.
 */
public class BetManagerTest {

    public static void main(String[] args) {
        String[] fighters = {"Bob", "Jim", "Gloopmaster"};
        BetManager bm = new BetManager(Arrays.asList(fighters));

        bm.addBet("Bob", "p1", 234.54);
        bm.addBet("Bob", "p2", 325.00);
        bm.addBet("Jim", "p3", 0);
        bm.addBet("Jim", "p4", 0);
        bm.addBet("Gloopmaster", "p5", 89.99);
        bm.addBet("Bob", "p1", 56.10);
        bm.addBet("Gloopmaster", "Gloopmaster", 138.22);

        Map<String, Double> winnings = bm.calcWins("Jim");
        for(String w : winnings.keySet()){
            System.out.println(w + ": " + winnings.get(w));
        }
    }
}
