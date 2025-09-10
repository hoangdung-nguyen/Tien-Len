package cardGames;

import java.util.*;

public class Deck extends Stack<Card>{
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
public Deck() {
	for(char r:Utils.RANK_ORDER.keySet()) {
		if(r==' ') {
			continue;
		}
		for(char s:Utils.SUIT_ORDER.keySet()) {
			push(new Card(r,s));
		}
	}
}
public void shuffle() {
	Collections.shuffle(this);
}

}
