package cardGames;

import java.util.*;

public class Card implements Comparable<Card>, java.io.Serializable{
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
char rank;
char suit; //0=spade 1=club 2=diamond 3=heart
public Card(char r,char s) {
	rank=r;
	suit=s;
}
public Card(String a) {
	this(a.charAt(0),a.charAt(1));
}
@Override
public int compareTo(Card o) {
    int cmp = Integer.compare(Utils.RANK_ORDER.get(this.rank), Utils.RANK_ORDER.get(o.rank));
    if (cmp != 0) return cmp;
    return Integer.compare(Utils.SUIT_ORDER.get(this.suit), Utils.SUIT_ORDER.get(o.suit));
}
@Override
public boolean equals(Object obj) {
    // Compare Card objects by their fields (rank, suit, etc.)
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Card other = (Card) obj;
    return this.rank == other.rank && this.suit==other.suit;
}

@Override
public int hashCode() {
    // Generate hashCode based on the Card fields
    return Objects.hash(rank, suit);
}
public String toString() {
	return ""+rank+suit;
}

}
