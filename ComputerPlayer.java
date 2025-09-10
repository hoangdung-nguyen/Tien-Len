package cardGames;

import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeSet;
import java.util.HashSet;

public class ComputerPlayer extends Player{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final double PRUNING_THRESHOLD = 0.2;
	private static final boolean DEBUG = false;
	Stack<TreeSet<Card>> playedCards;
	ArrayList<TreeSet<Card>> playerPlayed;
	ArrayList<MoveSequence> moveSequences;
	public ComputerPlayer(Stack<TreeSet<Card>> used,ArrayList<TreeSet<Card>> played) { //pass in the card stack and played
		super();
		moveSequences = new ArrayList<MoveSequence>();
		playedCards=used;
		playerPlayed = played;
	}
	public ComputerPlayer(String s) {
		super(s);
		moveSequences = new ArrayList<MoveSequence>();
		playedCards=new Stack<TreeSet<Card>>();
	}
	public ComputerPlayer(Player s) {
		super(s);
		moveSequences = new ArrayList<MoveSequence>();
		playedCards=new Stack<TreeSet<Card>>();
	}
	public void setStack(Stack<TreeSet<Card>> used) {
		playedCards=used;
	}
	
	class MoveSequence extends ArrayList<TreeSet<Card>> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		double score;
		MoveSequence(double score,TreeSet<Card> node) {
			this.score = score;
			add(node);
		}
		MoveSequence(double score,ArrayList<TreeSet<Card>> sequence) {
			super(sequence);
			this.score = score;
		}
		public String toString() {
			return super.toString()+" "+score;
		}
	}
	public void play(stackState state,ArrayList<Player> currentPlayers) {	// naive implementation, always plays most cards possible, smallest first
		if(DEBUG) System.out.println(this+". Moves: "+moves);
		deselectAll();
		if (moves.isEmpty()) {
			if(DEBUG) System.out.println("PASS "+current);
			ui.pass.doClick();
			return;
		}
		ArrayList<TreeSet<Card>> moves1 = new ArrayList<TreeSet<Card>>(moves);
		moves1.sort((move1, move2) -> {
			// Compare by size, descending
			int sizeCompare = Integer.compare(move2.size(), move1.size());
			if (sizeCompare != 0) return sizeCompare;

			// Compare by rank of first card, ascending
			int rankCompare = Integer.compare(
				Utils.RANK_ORDER.get(move1.first().rank), 
				Utils.RANK_ORDER.get(move2.first().rank)
			);
			if (rankCompare != 0) return rankCompare;

			// Compare by suit of last card, descending
			return Integer.compare(
				Utils.SUIT_ORDER.get(move2.last().suit), 
				Utils.SUIT_ORDER.get(move1.last().suit)
			);
		});	
		boolean someoneAboutToWin = false;
			for (Player p : currentPlayers) {
				if (p.size() == 1) {
					someoneAboutToWin = true;
					break;
				}
			}
		if (state == stackState.same && someoneAboutToWin) {
			select(moves1.get(moves.size()-1));
		}
		else select(moves1.get(0));
		if(DEBUG) System.out.println(current);
		ui.play.doClick();
	}
	public void play() { // the one using minimax
		if(current.isEmpty()) {
			ui.pass.doClick();
		}
		else{
			ui.play.doClick();
		}
		if(TienLen.DEBUG) {
			System.out.println("ComputerPlayer played: "+current);
		}
	}
	public void getMove(stackState state,TreeSet<Card>peek) { // the one using minimax
		deselectAll();
		TreeSet<Card> move = getMove(state, peek,2);
		if(move!=null) select(move);
		if(TienLen.DEBUG) {
			printMoveSequences();
		}
	}
	/* For reference:
	 * 
	Material count
	Piece mobility
	King safety
	Pawn structure
	Control of the center
	Other positional elements
	
	Though, minimax might not work because you don't actually know the other player's cards.
	I guess I can assume cards, but that's shittingly hard
	 */
	int i=0,cut=0;
	public TreeSet<Card> getMove(stackState state,TreeSet<Card>peek,int depth) {
		TreeSet<Card> bestMove = null;
		MoveSequence bestValue = null;
		moveSequences.clear();
		
		for (TreeSet<Card> move : moves) {
			MoveSequence value = minimax(this,move,state,depth,0,Double.NEGATIVE_INFINITY);
			value.score/=depth+1;
			if (bestMove==null||value.score > bestValue.score) {
				bestValue = value;
				bestMove = move;
			}
			moveSequences.add(value);
		}
		System.out.println(bestValue);
		if(bestValue==null || bestValue.score < evaluateMove(this,new TreeSet<Card>(),stackState.none,moves)) return null;
		return bestMove;
	}
	private MoveSequence minimax(TreeSet<Card> hand,TreeSet<Card> move,stackState state, int depth, double accumulatedScore, double bestScore) {
		i++;
		if(DEBUG) System.out.println(i+". Evaluating: " + move+" in "+ hand + " depth: "+ depth);
		TreeSet<Card> copy=new TreeSet<Card>(hand);
		copy.removeAll(move);
		stackState newMoveState = getMoveState(move);
		HashSet<TreeSet<Card>> moves=new HashSet<TreeSet<Card>>();
		getMoves(copy,moves);
		double currentScore = evaluateMove(copy,move,state,moves);
		if(currentScore < bestScore * PRUNING_THRESHOLD) {
			++cut;
			if(DEBUG)System.out.println("PRUNED:"+move+" at "+hand);
		}
		if (depth == 0 || moves.isEmpty() || currentScore < bestScore * PRUNING_THRESHOLD) {

			return new MoveSequence(accumulatedScore+(currentScore/ Math.pow(0.9, depth))*(depth+1),move);
		}
		double maxVal = Double.NEGATIVE_INFINITY;
		ArrayList<TreeSet<Card>> newSequence = new ArrayList<>();
		for (TreeSet<Card> nextMove : moves) {
			MoveSequence eval = minimax(copy,nextMove,newMoveState, depth - 1,accumulatedScore+currentScore/Math.pow(0.9, depth),maxVal);
			if (eval.score > maxVal) {
				maxVal = eval.score;
				newSequence = new ArrayList<>(eval);
			}
		}
		newSequence.add(0,move);
		if(DEBUG) System.out.println(maxVal+" "+move+" in "+ hand);
		return new MoveSequence(maxVal,newSequence);
	}
	private TreeSet<Card> simulateOpponents(){
		System.out.println("THIS IS OPPONENT:"+playerPlayed);
		
		for (int i=0;i<4;i++) {
			// AHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
			// WHYYYYYYYYY
			// THIS SHIT FUCKING HARD AS FUCKKKK
			// 
			
		}
		return new TreeSet<Card>();
	}
	private double evaluateMove(TreeSet<Card> hand,TreeSet<Card> movePlayed,stackState state,HashSet<TreeSet<Card>> moves) {
		// Ok, so plan, check each for if played get moves, check for count, 
		//AHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH HOW DO YOU EXPECT ME TO DO THIS
		// IF you ever think we can set up a neural net, we can try to make it play the naive implementation, then play it yourself until its good
		double score = 10 * Math.log(1 + moves.size());
		if(hand.size()==0) score += 1000;
		score += Math.pow(1.5,(13-hand.size()));
		for (Card card : hand) {
			score += (Utils.RANK_ORDER.get(card.rank)*4 + Utils.SUIT_ORDER.get(card.suit)) / hand.size() * 5;
		}
		for (TreeSet<Card> move : moves) {
			score += move.size() / moves.size();
		}
		stackState moveState = getMoveState(movePlayed);
		HashSet<TreeSet<Card>> sameStateMoves = new HashSet<TreeSet<Card>>();
		if(!movePlayed.isEmpty()) {
		getValidMoves(hand,sameStateMoves,moveState,movePlayed);
		for(TreeSet<Card> move : sameStateMoves) {
			score +=  moveState==stackState.none? Math.log(move.size()) : move.size();
		}
		}
		if (moveState == stackState.hang && (state == stackState.pig || state == stackState.same)) score += 250;
		return score;
	}
	public void printMoveSequences() {
		System.out.println(i+" "+cut);
		moveSequences.sort((s1, s2) -> {return Double.compare(s2.score, s1.score);});
		System.out.println(evaluateMove(this,new TreeSet<Card>(),stackState.none,moves));
		for(ArrayList<TreeSet<Card>> moveS : moveSequences){
			System.out.println(moveS);
		}
		i=0;cut=0;
	}
	static int log2(int n)
	{
	return (n==1)? 0 : 1 + log2(n/2);
	}
	public static void main(String[] args) {
		//Player a=new ComputerPlayer("[Jd, Jh, Qs, Qd, Qh, Kc, Kd, 1s, 1c, 1d, 1h, 2d, 2h]");
		ComputerPlayer a1 =new ComputerPlayer("[6d, 8c, 9c, 9d, 9h, 0s, 0d, Js, Jh, Qc, Kd, 1s, 1c]");
		TreeSet<Card> b=new TreeSet<Card>();
		b.add(new Card('1','d'));
		b.add(new Card('1','c'));
		System.out.println(getMoveState(new TreeSet<Card>()));
		a1.getValidMoves(stackState.none, null);
		System.out.println(a1);
		System.out.println(((ComputerPlayer) a1).getMove(stackState.none, null,4));
		((ComputerPlayer) a1).printMoveSequences();
		a1.getValidMoves(stackState.none, null);
		System.out.println(a1);
		System.out.println(a1.moves);
	}
}
