package cardGames;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Player extends TreeSet<Card>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
HashSet<TreeSet<Card>> moves;
TreeSet<Card> current;
//used in TienLen only. Useless here because I don't handle UI here, but I want to have it for easier access.
int score;	
UISet ui;
public Player() {
	moves=new HashSet<TreeSet<Card>>();
	current=new TreeSet<Card>();
}
public Player(String a) {
	moves=new HashSet<TreeSet<Card>>();
	current=new TreeSet<Card>();
	Pattern pattern = Pattern.compile("[1234567890JQK][scdh]"); //finds the toString of the cards to convert to Card
	Matcher matcher = pattern.matcher(a);
	while (matcher.find()){
		int e=matcher.start();
		add(new Card(a.charAt(e),a.charAt(e+1)));
	}

}
public Player(Player p) {
	this();
	addAll(p);
}
//public ComputerPlayer toComputerPlayer() {
//	ComputerPlayer out=new ComputerPlayer(this);
//	return out;
//}
private static boolean isSameRank(TreeSet<Card> m) {
	for(Card c:m) {
		if(m.higher(c)!=null&&c.rank!=m.higher(c).rank) {
			return false;
		}
	}
	return true;
}
static boolean isPigs(TreeSet<Card> m,int pigNum) {
	return m.size()==pigNum&&m.first().rank=='2'&&isSameRank(m);
}
private static boolean isFour(TreeSet<Card> m) { // Probably never even runs because isSame() is always checked first
	if (m.size()!=4) return false;
	for(Card c:m) {
		if(m.higher(c)!=null&&c.rank!=m.higher(c).rank) {
			return false;
		}
	}
	if(TienLen.DEBUG) System.out.println("Co Hang!: "+m.first().rank);
	return true;
}

private static boolean isRun(TreeSet<Card> m) {
	for(Card c:m) {
		if(m.higher(c)!=null&&Utils.RANK_ORDER.get(c.rank)!=Utils.RANK_ORDER.get(m.higher(c).rank)-1) {
			return false;
		}
	}
	return true;
}
private static boolean isHang(TreeSet<Card> m,int pigNum,boolean save) {
	if(save&&m.size()<4+2*pigNum) return false;
	TreeMap<Character,Integer> pairsOf=new TreeMap<Character,Integer>((card1, card2) -> {
	    return Utils.RANK_ORDER.get(card1) - Utils.RANK_ORDER.get(card2);
	});
	for(Card c:m) {
		if(!pairsOf.containsKey(c.rank)) {
			pairsOf.put(c.rank, 1);
		}
		else {
			pairsOf.put(c.rank, pairsOf.get(c.rank)+1);
		}
		if(pairsOf.get(c.rank)>2) {
			return false;
		}
	}
	if(save&&pairsOf.size()<3) { //if theres enough
		return false;
	}
	char prev='x';
	for(char rank:pairsOf.keySet()) {
		if(save&&pairsOf.get(rank)!=2) {
			return false;
		}
		if(prev!='x'&&(pairsOf.get(rank)!=2||Utils.RANK_ORDER.get(prev)!=Utils.RANK_ORDER.get(rank)-1)) {
			return false;
		}
		prev=rank;
	}
	if(save&&TienLen.DEBUG) System.out.println("Co Hang!: "+pairsOf);
	return true;
}
int i=0,cut=0;
private void getMoves(TreeSet<Card>hand,HashSet<TreeSet<Card>> moves,TreeSet<Card> m, Set<Card> visited, 
        BiPredicate<TreeSet<Card>,TreeSet<Card>> isValid, 
        BiPredicate<TreeSet<Card>, TreeSet<Card>> meetsCriteria, 
        TreeSet<Card> peek) {
	i++;
	if (m.isEmpty()||isValid.test(m,peek)) {
		cut++;
		if (!m.isEmpty() && !moves.contains(m) && meetsCriteria.test(m, peek)) {
			moves.add(new TreeSet<>(m));
		}
		for (Card c : hand) {
			if (visited.contains(c)) continue;
			visited.add(c);
			TreeSet<Card> newSet = new TreeSet<>(m);
			newSet.add(c);
			getMoves(hand,moves,newSet, visited, isValid, meetsCriteria, peek);
			visited.remove(c);
		}
	}
}
public void getMoves(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves) {
	getMoves(hand,moves,new TreeSet<Card>(),new TreeSet<Card>(),
			(m,peek)->(isRun(m)||isHang(m, 0, false)||isSameRank(m)),
			(m,peek)->(isRun(m)&&m.size()>2)||isHang(m,1,true)||isSameRank(m),
			new TreeSet<Card>());
}
public void getMoves() {
	getMoves(this,moves);
}
public void getMovesThatContains(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves,Card c) {
	TreeSet<Card>tempPeek=new TreeSet<Card>();
	tempPeek.add(c);
	getMoves(hand,moves,new TreeSet<Card>(),new TreeSet<Card>(),
			(m,peek)->m.contains(peek.last())&&(isHang(m, 0, false)||isSameRank(m)||isRun(m)),
			(m,peek)->(isRun(m)&&m.size()>2)||isHang(m,1,true)||isSameRank(m),
			tempPeek);
}
public void getMovesThatContains(Card lowest) {
	moves.clear();
	getMovesThatContains(this,moves,lowest);
}
public void getRuns(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves,TreeSet<Card> tempPeek) {
	getMoves(hand,moves,new TreeSet<Card>(),new TreeSet<Card>(),
			(m,peek)->m.size()<=peek.size()&&isRun(m),
			(m,peek)->m.size()==peek.size()&&m.last().compareTo(peek.last())>0,
			tempPeek);
}
public void getSames(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves,TreeSet<Card> tempPeek) {
	getMoves(hand,moves,new TreeSet<Card>(),new TreeSet<Card>(),
			(m,peek)->m.size()<=peek.size()&&isSameRank(m),
			(m,peek)->m.size()==peek.size()&&m.last().compareTo(peek.last())>0,
			tempPeek);
}
public void getPigs(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves,TreeSet<Card> tempPeek) {
	getMoves(hand,moves,new TreeSet<Card>(),new TreeSet<Card>(),
			(m,peek)->isSameRank(m)||isHang(m, 0, false),
			(m,peek)->(peek.size()==1&&isFour(m))||(isPigs(m,peek.size())&&m.last().compareTo(peek.last())>0)||isHang(m,peek.size(),true),
			tempPeek);
}
public void getPigs() {
	getPigs(this,moves);
}
public void getPigs(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves) {
	TreeSet<Card>tempPeek=new TreeSet<Card>();
	tempPeek.add(new Card('3','s'));
	getMoves(hand,moves,new TreeSet<Card>(),new TreeSet<Card>(),
			(m,peek)->isSameRank(m)||isHang(m, 0, false),
			(m,peek)->(peek.size()==1&&isFour(m))||(isPigs(m,peek.size())&&m.last().compareTo(peek.last())>0)||isHang(m,peek.size(),true),
			tempPeek);
}
public void getHang(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves,TreeSet<Card> tempPeek) {
	getMoves(hand,moves,new TreeSet<Card>(),new TreeSet<Card>(),
			(m,peek)->m.size()<=peek.size()&&((isSameRank(m))||isHang(m, 0, false)),
			(m,peek)->((peek.size()==4&&isFour(m))||isHang(m,(peek.size()-4)/2,true))&&m.last().compareTo(peek.last())>0,
			tempPeek);
}
public stackState getMoveState() {
	return getMoveState(current);
}
public static stackState getMoveState(TreeSet<Card> move) {
	if(move.isEmpty()) return stackState.none;
	if(move.first().rank=='2') return stackState.pig;
	if(isSameRank(move)) return stackState.same;
	if(isRun(move)) return stackState.run;
	if(isHang(move,1,true)||isFour(move)) return stackState.hang;
	return stackState.none;
}
public void getValidMoves(TreeSet<Card> hand,HashSet<TreeSet<Card>> moves,stackState state,TreeSet<Card> peek) {
	//	System.out.println(state);
	moves.clear();
	switch(state) {
	case none: //when all skip and stack cleared
		getMoves(hand,moves);
		break;
	case pig: // pig reached when 2 is played.
		getPigs(hand,moves,peek);
		break;
	case run:
		getRuns(hand,moves,peek);
		break;
	case same:
		getSames(hand,moves,peek);
		break;
	case hang:
		getHang(hand,moves,peek);
		break;
	default:
		//TODO error cuz not supposed to get here
		break;
	}
	//System.out.println(currentPlayer.moves);
}
public void getValidMoves(stackState state,TreeSet<Card> peek) {
	getValidMoves(this,moves,state,peek);
}
public void printMoves() {
	moves.clear();i=0;cut=0;
	getMoves(this,moves);
	System.out.println(i+" "+cut); //These are pruning stats, pretty cool to look at.
	System.out.println(moves);
}
public boolean selectAll() {
	return current.addAll(this);
}
public boolean select(TreeSet<Card> c) {
	return current.addAll(c);
}
public boolean selectCard(Card c) {
	return current.add(c);
}
public boolean deselectCard(Card c) {
	return current.remove(c);
}
public boolean selectCard(JCard c) {
	return current.add(c.card);
}
public boolean deselectCard(JCard c) {
	return current.remove(c.card);
}
public void deselectAll() {
	current.clear();
}
public void clearAll() {
	clear();
	current.clear();
	moves.clear();
}
public static void main(String[] args) {
	Player a=new Player("[3s, 3c, 3d, 6c, 8s, 8d, 8h, 9c, Qs, Kh, 1h, 2c, 2h]");
	a.getMovesThatContains(TienLen.LOWEST_CARD);
	System.out.println(a.moves);
	System.out.println(getMoveState(new TreeSet<Card>()));
}
{ //grave
//	private static boolean isPotentialHang(TreeSet<Card> m) {
//		TreeMap<Character,Integer> pairsOf=new TreeMap<Character,Integer>((card1, card2) -> {
//		    return Utils.RANK_ORDER.get(card1) - Utils.RANK_ORDER.get(card2);
//		});
//		for(Card c:m) { //checks 1 or 2 of each card only
//			if(!pairsOf.containsKey(c.rank)) {
//				pairsOf.put(c.rank, 1);
//			}
//			else {
//				pairsOf.put(c.rank, pairsOf.get(c.rank)+1);
//			}
//			if(pairsOf.get(c.rank)>2) {
//				return false;
//			}
//		}
//		char prev='x';
//		for(char rank:pairsOf.keySet()) { //checks consecutive
//			//System.out.println(prev+" vs "+rank);
//			if(prev!='x'&&(pairsOf.get(prev)!=2||Utils.RANK_ORDER.get(prev)!=Utils.RANK_ORDER.get(rank)-1)) {
//				return false;
//			}
//			prev=rank;
//		}
//		//System.out.println("I: "+pairsOf);
//		return true;
//	}
	//public void getMoves( TreeSet<Card> m,Set<Card> visited) { //I'd say this is pretty well pruned! :D
////		System.out.println("CHECKED: "+m);
//		i++;
//		boolean run=isRun(m),pot=isPotentialHang(m),same=isSameRank(m);
//		if(pot||same||run){
//			cut++;
//			if(!m.isEmpty()&&!moves.contains(m)&&((run&&m.size()>2)||(pot&&isHang(m,1))||same)) {
////				System.out.println("Passed: "+m);
//				TreeSet<Card> newMove = new TreeSet<Card>();
//			    newMove.addAll(m);
//			    moves.add(newMove);
//			}
//			for(Card c:this){
//				if (visited.contains(c)) {continue;}
//				visited.add(c);
//				TreeSet<Card> newSet = new TreeSet<>(m);
//				newSet.add(c);
//				getMoves(newSet,visited);
//				visited.remove(c);
//			}
//		}
	//}
	//public void getMovesThatContains( TreeSet<Card> m,Set<Card> visited,TreeSet<Card> peek) { //Just making this to use once per program :')
////		System.out.println("CHECKED: "+m);
//		i++;
//		boolean run=isRun(m),pot=isPotentialHang(m),same=isSameRank(m);
//		if((m.isEmpty()||m.contains(peek.last()))&&(pot||same||run)){
//			cut++;
//			if(!m.isEmpty()&&!moves.contains(m)&&((run&&m.size()>2)||(pot&&isHang(m,1))||same)) {
////				System.out.println("Passed: "+m);
//				TreeSet<Card> newMove = new TreeSet<Card>();
//			    newMove.addAll(m);
//			    moves.add(newMove);
//			}
//			for(Card c:this){
//				if (visited.contains(c)) {continue;}
//				visited.add(c);
//				TreeSet<Card> newSet = new TreeSet<>(m);
//				newSet.add(c);
//				getMovesThatContains(newSet,visited,peek);
//				visited.remove(c);
//			}
//		}
	//}
	//public void getRuns( TreeSet<Card> m,Set<Card> visited,TreeSet<Card> peek) {
////		System.out.println("CHECKED: "+m);
//		i++;
//		if(m.size()<=peek.size()&&isRun(m)){
//			cut++;
//			if(!moves.contains(m)&&m.size()==peek.size()&&m.last().compareTo(peek.last())>0) {
////				System.out.println("Passed: "+m);
//				TreeSet<Card> newMove = new TreeSet<Card>();
//			    newMove.addAll(m);
//			    moves.add(newMove);
//			}
//			for(Card c:this){
//				if (visited.contains(c)) {continue;}
//				visited.add(c);
//				TreeSet<Card> newSet = new TreeSet<>(m);
//				newSet.add(c);
//				getRuns(newSet,visited,peek);
//				visited.remove(c);
//			}
//		}
	//}
	//public void getSames( TreeSet<Card> m,Set<Card> visited,TreeSet<Card> peek) { 
////		System.out.println("CHECKED: "+m);
//		i++;
//		if(m.size()<=peek.size()&&isSameRank(m)){
//			cut++;
//			if(m.size()==peek.size()&&!moves.contains(m)&&m.last().compareTo(peek.last())>0) {
////				System.out.println("Passed: "+m);
//				TreeSet<Card> newMove = new TreeSet<Card>();
//			    newMove.addAll(m);
//			    moves.add(newMove);
//			}
//			for(Card c:this){
//				if (visited.contains(c)) {continue;}
//				visited.add(c);
//				TreeSet<Card> newSet = new TreeSet<>(m);
//				newSet.add(c);
//				getSames(newSet,visited,peek);
//				visited.remove(c);
//			}
//		}
	//}
	//public void getPigs( TreeSet<Card> m,Set<Card> visited,TreeSet<Card> peek) {//for when pig is initially run
////		System.out.println("CHECKED: "+m);
//		i++;
//		if(isSameRank(m)||isPotentialHang(m)){
//			cut++;
//			if(!m.isEmpty()&&!moves.contains(m)&&((peek.size()==1&&isFour(m))||(isPigs(m,peek.size())&&m.last().compareTo(peek.last())>0)||isHang(m,peek.size()))) {
////				System.out.println("Passed: "+m);
//				TreeSet<Card> newMove = new TreeSet<Card>();
//			    newMove.addAll(m);
//			    moves.add(newMove);
//			}
//			for(Card c:this){
//				if (visited.contains(c)) {continue;}
//				visited.add(c);
//				TreeSet<Card> newSet = new TreeSet<>(m);
//				newSet.add(c);
//				getPigs(newSet,visited,peek);
//				visited.remove(c);
//			}
//		}
	//}
	//public void getHang( TreeSet<Card> m,Set<Card> visited,TreeSet<Card> peek) {
////		System.out.println("CHECKED: "+m);
//		i++;
//		if(m.size()<=peek.size()&&((isSameRank(m))||isPotentialHang(m))){
//			cut++;
//			if(!m.isEmpty()&&!moves.contains(m)&&((peek.size()==4&&isFour(m))||isHang(m,(peek.size()-4)/2))&&m.last().compareTo(peek.last())>0) {
////				System.out.println("Passed: "+m);
//				TreeSet<Card> newMove = new TreeSet<Card>();
//			    newMove.addAll(m);
//			    moves.add(newMove);
//			}
//			for(Card c:this){
//				if (visited.contains(c)) {continue;}
//				visited.add(c);
//				TreeSet<Card> newSet = new TreeSet<>(m);
//				newSet.add(c);
//				getHang(newSet,visited,peek);
//				visited.remove(c);
//			}
//		}
	//}
}
}
