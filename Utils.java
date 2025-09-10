package cardGames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
enum stackState implements Serializable{
	same,
	run,
	pig,
	hang,
	none,
}
enum MessageType { JOIN, PLAY, PASS, UPDATE, DISCONNECT , PLACE}
	
public class Utils {
	final static Card LOWEST_CARD = new Card('3','s');
	static final int MAX_PLAYERS = 4;
	static final int CARDS_PER_PLAYER = 13;
	static final HashMap<Character, Integer> RANK_ORDER = new HashMap<>();
    static {
    	char[] rankOrder={'3','4','5','6','7','8','9','0','J','Q','K','1',' ','2'};
    	for (int i=0; i<rankOrder.length;i++) {
            RANK_ORDER.put(rankOrder[i],i);
        }
    }
    static final HashMap<Character, Integer> SUIT_ORDER = new HashMap<>();
    static {
    	char[] suitOrder={'s','c','d','h'};
    	for (int i=0;i<suitOrder.length;i++) {
            SUIT_ORDER.put(suitOrder[i],i);
        }
    }
    private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static int toDecimal(String number, int from)
    {
        int result = 0;
        int position = number.length();        
        for (char ch : number.toCharArray())
        {
            int value = SYMBOLS.indexOf(ch);
            result += value * Math.pow(from,--position);
        }
        return result;
    }
    public static String changeBase(String number, int from, int to)
    {
        int result = 0;
        int position = number.length();        
        for (char ch : number.toCharArray())
        {
            int value = SYMBOLS.indexOf(ch);
            result += value * Math.pow(from,--position);
        }
        return Integer.toString(result,to);
    }
    public static String toString(stackState state) {
    	switch(state) {
		case hang: return "hang";
		case pig: return "pig";
		case run: return "run";
		case same: return "same";
    	}
    	return "none";
    }
}
class UISet{
	JButton play,pass;
	JPanel panel,cardsPane;
	Map<Card,JCard>cards;
	int cardsNum;
}
/*
 * Needs: 
 * currentplayer
 * currentStack
 * TreeSet<Card> hand,TreeSet<Card> movePlayed,stackState state,HashSet<TreeSet<Card>> moves
 */
class ServerMessage implements Serializable {
	private static final long serialVersionUID = 1L;
		
	private MessageType type;
	private Object data;

	public ServerMessage(MessageType type, Object data) {
		this.type = type;
		this.data = data;
	}

	public MessageType getType() { return type; }
	public Object getData() { return data; }
	//readobject, writeobject
}
/*
 * Actions:
 * send treeset
 * send pass
 * send login?
 * send message
 */
class ClientMessage implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private MessageType type;
	private Object data;

	public ClientMessage(MessageType type, Object data) {
		this.type = type;
		this.data = data;
	}
	public MessageType getType() { return type; }
	public Object getData() { return data; }
}
class GameState implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<Integer> handSizes;
	int currentPlayerID;
	stackState state; 
	
	GameState(ArrayList<Player> players,int id,stackState state){
		handSizes = new ArrayList<Integer>();
		for(Player p:players) {
			handSizes.add(p.size());
		}
		currentPlayerID = id;
		this.state= state;
	}
	public String toString() {
		return handSizes.toString() + currentPlayerID + Utils.toString(state);
	}
}
