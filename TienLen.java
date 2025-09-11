package cardGames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeSet;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class TienLen extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final static boolean DEBUG=false;
	final static boolean UIDEBUG=true;
	private static final int PORT = 5000;
	final static Card LOWEST_CARD = new Card('3','s');
	private final transient ServerSocket serverSocket;
	
	private Color backColor, midColor1, midColor2, textColor;
	private JPanel table,cardsPanel,table1;
	private JTextArea temporaryDebugDisplay;
	private JButton ComputerPlayerPlay;
	
	private ArrayList<TreeSet<Card>> playerPlayed;
	private Deck bai;
	private Stack<TreeSet<Card>> currentStack;
	private Player currentPlayer;
	private ArrayList<Player> playersInPlay,playersInGame;
	final ArrayList <Player>players;
	private Player winner;
	boolean waitingForMove, gameStarted = false;
	int startX,startY,cardWidth,cardHeight;
	stackState state;
	boolean robotAutoRun=false;

	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		final TienLen[] guiHolder = new TienLen[1];
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					guiHolder[0]=new TienLen(Color.DARK_GRAY);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		TienLen thisInstance = guiHolder[0];
		thisInstance.waitForPlayers();
		thisInstance.chiaBai();
		thisInstance.startArraysSetup();
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					thisInstance.startGameSetup();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	public TienLen(Color back) throws IOException {
		if(UIDEBUG)System.out.println("public TienLen(Color back) throws IOException {");
		serverSocket = new ServerSocket(PORT);
		System.out.println("Tien Len Server is running...");		
		backColor=back;
		initializePlayArea();
		bai=new Deck();
		bai.shuffle();
		players=new ArrayList<Player>();
		repaint();
		setBounds(25, 100, 1001, 1000);
		currentStack=new Stack<TreeSet<Card>>();
		playerPlayed = new ArrayList<TreeSet<Card>>();
		for (int i = 0;i<4;i++) {
			playerPlayed.add(new TreeSet<Card>());
		}
		//Debug hand
//		if (!DEBUG) {
//			players.add(new ComputerPlayer("[3h, 4c, 9s, 9d, 9h, 0c, Qc, Kh, 1h, 2s, 2c, 2d, 2h]"));
//			players.add(new ComputerPlayer("[4s, 4d, 5d, 6d, 7d, 8h, 0h, Js, Jd, Jh, Qh, 1s, 1d]"));
//			players.add(new ComputerPlayer("[3s, 3c, 3d, 5c, 6s, 6c, 7s, 7c, 8s, 9c, 0d, Jc, 1c]"));
//			players.add(new ComputerPlayer("[4h, 5s, 5h, 6h, 7h, 8c, 8d, 0s, Qs, Qd, Ks, Kc, Kd]"));
//		}
//		else {
//			for(int i=0;i<4;i++) {
//				players.add(new ComputerPlayer(currentStack));
//				players.get(i).score=0;
//			}
//		}
	}
	
	private void initializePlayArea(){
		if(UIDEBUG)System.out.println("private void initializePlayArea(){");
		// Making window presets
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(25, 100, 1000, 1000);
		getContentPane().setSize(5000,5000);
		setLayout(null);
		getContentPane().setBackground(backColor);
		//inverse of screen color
		midColor1 = new Color((255-backColor.getRed()+2*backColor.getRed())/3,(255-backColor.getGreen()+2*backColor.getGreen())/3,(255-backColor.getBlue()+2*backColor.getBlue())/3);
		midColor2 = new Color((2*(255-backColor.getRed())+backColor.getRed())/3,(2*(255-backColor.getGreen())+backColor.getGreen())/3,(2*(255-backColor.getBlue())+backColor.getBlue())/3);
		textColor = new Color(255-backColor.getRed(),255-backColor.getGreen(),255-backColor.getBlue());
		setVisible(true);
		table=new JPanel(new BorderLayout());
		normalize(table);
		cardsPanel=new JPanel(null);
		normalize(cardsPanel);
		if(UIDEBUG)temporaryDebugDisplay=new JTextArea();
		table1=new JPanel();
		normalize(table1);
		table1.setBackground(midColor1);
		table.add(table1,BorderLayout.CENTER);
		if(DEBUG)table1.add(temporaryDebugDisplay,BorderLayout.CENTER);
		add(table);
		add(cardsPanel);
		table.setOpaque(false);
		cardsPanel.setOpaque(false);
		setComponentZOrder(table,0);
		setComponentZOrder(cardsPanel,0);
		//table.setBounds(getBounds());
		ComputerPlayerPlay = new JButton("New Game");
		normalize(ComputerPlayerPlay);
		cardsPanel.add(ComputerPlayerPlay);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				// resizing panels
				Insets inset=getInsets();
				table.setBounds(inset.left,inset.top,getWidth()-inset.left-inset.right,getHeight()-inset.top-inset.bottom);
				cardsPanel.setBounds(inset.left,inset.top,getWidth()-inset.left-inset.right,getHeight()-inset.top-inset.bottom);
				ComputerPlayerPlay.setBounds((getWidth()-inset.left-inset.right-50)/2,(getHeight()-inset.top-inset.bottom-50)/2,50,50);
				repaint();
			}
		});
		cardsPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
	  		resizeCards();
				if (gameStarted) {
					for (Player p:players) {
						reformatCards(p);
					}
				}
			}
		});
		ComputerPlayerPlay.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(robotAutoRun && !waitingForMove) waitingForMove = true;
				else waitingForMove = false;
			}
		});
	}

	private void waitForPlayers() {
		if(UIDEBUG)System.out.println("private void waitForPlayers() {");
		if(!robotAutoRun) waitingForMove = true;
		// Start listening for connections in a separate thread
		Thread acceptThread = new Thread(() -> {
			while (players.size() < Utils.MAX_PLAYERS && waitingForMove) {
				try {
					Socket socket = serverSocket.accept();
					OnlinePlayer player = new OnlinePlayer(socket, players.size(), this);
					players.add(player);
					players.get(players.size()-1).score=0;
					new Thread(player).start();
					player.sendMessage(player);
					System.out.println("Player " + (players.size()-1) + " connected.");
				} catch (IOException e) {
					if (!waitingForMove) break; // If manually started, exit the loop
					e.printStackTrace();
				}
			}
		});
		acceptThread.start(); // Start listening for players

		// Wait until start is requested or 4 players join
		while (players.size() < Utils.MAX_PLAYERS && waitingForMove) {
			try {
				Thread.sleep(1000); // Prevent CPU overuse
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int i=0;
		while (players.size() < Utils.MAX_PLAYERS) {
			i++;
			players.add(new ComputerPlayer(currentStack,playerPlayed));
		}
		System.out.println("Game starting with " + (players.size() - i) + " players.");
		gameStarted = true;
	}

	private void gameLoop() throws IOException {
		handCheck();
		while (playersInGame.size()>1) {
			if(DEBUG) System.out.println(currentPlayer.moves);
			waitForPlayerMove();
			winnerCheck();
			updateGameDisplay();
			highlightCurrentPlayer();
			currentPlayer.getValidMoves(state,(currentStack.isEmpty())?null:currentStack.peek());
		}
		for(Player play:players) thuiHeo(play);
		for(int i=0;i<Utils.MAX_PLAYERS;i++) {
			System.out.println("Player "+i+": "+players.get(i).score);
		}
		end();
		newGame();
	}

	private void startGameSetup() throws IOException {
		if(UIDEBUG)System.out.println("private void startGameSetup() throws IOException {");
		broadcastUpdate();
		broadcast(new ServerMessage(MessageType.JOIN, new GameState(players,playersIndexOf(currentPlayer),state)));
		for(int i=0;i<Utils.MAX_PLAYERS;i++) initializePlayerArea(i);
		// I would love to put these in initPlayArea, but these need to be after the cards' bounds are set so they can all run componentResized when this is set.
		highlightCurrentPlayer();
		broadcast(new ServerMessage(MessageType.PLAY, new GameState(players,playersIndexOf(currentPlayer),state)));
		for (Player p:players) {
			reformatCards(p);
		}
		if(currentPlayer instanceof ComputerPlayer) computerPlay((ComputerPlayer)currentPlayer);
	}

	private void startArraysSetup() {
		if(UIDEBUG)System.out.println("private void startArraysSetup() {");
		playersInGame=new ArrayList<Player>(players);
		playersInPlay=new ArrayList<Player>(playersInGame);
		state=stackState.none;
		currentPlayer.getMovesThatContains(Utils.LOWEST_CARD);
	}
	private void resizeCards() { //resizes cards on player hands.
		if(UIDEBUG)System.out.println("private void resizeCards() { ");
		startX = (getWidth()-getInsets().left-getInsets().right)/15;
		startY = (getHeight()-getInsets().top-getInsets().bottom)/15;
		if(getWidth()<getHeight()) {
			cardWidth = (getWidth()-getInsets().left-getInsets().right)/8;
			cardHeight = (int)(cardWidth*JCard.getRatio());
		}
		else {
			cardWidth = (getHeight()-getInsets().top-getInsets().bottom)/8;
			cardHeight = (int)(cardWidth*JCard.getRatio());
		}
	}

	public void chiaBai() {
		if(UIDEBUG)System.out.println("public void chiaBai() {");
		while(!bai.isEmpty()) { // Chia bai
			for(Player p:players) {
				p.add(bai.pop());
			}
		}
		for(Player p : players) {
			if(p.first().equals(Utils.LOWEST_CARD)) currentPlayer=p;
		}
	}

	private void initializePlayerPanels(int i) {
		if(UIDEBUG)System.out.println("private void initializePlayerPanels(int i) {");
		Player p=players.get(i);
		p.ui = new UISet();
		JPanel temp; 
		//Fine, we'll make some temps for readability
		int startX=this.startX,startY=this.startY;
		int areaWidth=startX*Utils.CARDS_PER_PLAYER-2*cardHeight;
		int overlap = (startX*Utils.CARDS_PER_PLAYER-cardWidth-2*cardHeight)/12;
		switch(i) { // 4 players, each a side
		case 0: temp=new JPanel(new GridLayout(0,2));table.add(temp,BorderLayout.PAGE_START);
		startX+=cardHeight;
		break;
		case 1: temp=new JPanel(new GridLayout(0,1));table.add(temp,BorderLayout.LINE_END);
		startX = getWidth()-getInsets().left-getInsets().right-startX-cardHeight;
		startY+=cardHeight;
		break;
		case 2: temp=new JPanel(new GridLayout(0,2));table.add(temp,BorderLayout.PAGE_END);
		startX+=cardHeight;
		startY = getHeight()-getInsets().top-getInsets().bottom-startY-cardHeight;
		break;
		case 3: temp=new JPanel(new GridLayout(0,1));table.add(temp,BorderLayout.LINE_START);
		startY+=cardHeight;
		break;
		default:temp=new JPanel();
		}
		p.ui.panel=temp;int index = 0;p.ui.cardsPane=new JPanel();p.ui.cardsPane.setLayout(null);p.ui.cardsPane.setOpaque(false);
		normalize(temp);
		HashMap<Card,JCard> cardButs=new HashMap<Card,JCard>();
		for(Card c:p) { // Adding cards as checkboxes and those map onto a hashmap
			JCard box=new JCard(c);
			box.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if(e.getStateChange()==1) p.selectCard(c);
					else p.deselectCard(c);
				}
			});
			box.setOpaque(false);
			box.setBounds(index * (i%2==0?overlap:0),index * (i%2==1?overlap:0), i%2==0?cardWidth:cardHeight, i%2==0?cardHeight:cardWidth);
			p.ui.cardsPane.add(box, (i>1)?0:index);
			if(i%2==1) box.rotate(90);
			cardButs.put(c,box);
			index++;
		}
		cardsPanel.add(p.ui.cardsPane,i);
		p.ui.cardsPane.setBounds(startX,startY,i%2==0?areaWidth:cardHeight,i%2==1?areaWidth:cardHeight);
		p.ui.cards=cardButs;
	}

	private void playHang() {
		if(UIDEBUG)System.out.println("private void playHang() {");
		System.out.println("YOU GOT GOT");
		Stack<TreeSet<Card>> theGotten=new Stack<TreeSet<Card>>();
		while(!currentStack.isEmpty()){
			theGotten.push(currentStack.pop());
			if(theGotten.peek().size()<=3)break;
		}
		int temp1=(playersInPlay.indexOf(currentPlayer)-1);
		for(Card c:theGotten.peek()) {
			players.get((temp1<0)?temp1+playersInPlay.size():temp1%playersInPlay.size()).score-=(Utils.SUIT_ORDER.get(c.suit)+1)*theGotten.size();
			currentPlayer.score+=(Utils.SUIT_ORDER.get(c.suit)+1)*theGotten.size();
		}
		while(!theGotten.isEmpty()) {
			currentStack.push(theGotten.pop());
		}
		if(DEBUG) for(int i=0;i<Utils.MAX_PLAYERS;i++) System.out.println("Player "+i+": "+players.get(i).score);
	}

	private void initializePlayerArea(int i) {
		if(UIDEBUG)System.out.println("private void initializePlayerArea(int i) {");
		Player p=players.get(i);
		if(DEBUG) {
			System.out.println("\n"+p);
			p.printMoves();
		}
		initializePlayerPanels(i);
		JButton pass=new JButton("Pass");
		normalize(pass);
		pass.addActionListener(new ActionListener(){ // The pass button
			@Override
			public void actionPerformed(ActionEvent e) {
				if(state!=stackState.none) {
					playersInPlay.remove(currentPlayer);
					waitingForMove = false;
//					System.out.println(playersInPlay.size());
					}
				else {
					//TODO error
				}
			}
		});
		JButton play=new JButton("Play");
		normalize(play);
		play.addActionListener(new ActionListener(){ // The play button
			@Override
			public void actionPerformed(ActionEvent e) {
				if(DEBUG)System.out.println("THIS IS PRESSED "+currentPlayer.moves+currentPlayer.current);
				TreeSet<Card> selectedCards = currentPlayer.current;
				if (currentPlayer.moves.contains(selectedCards)) {
					if(DEBUG)System.out.println(state);
					if(currentPlayer.getMoveState()!=stackState.none) state=currentPlayer.getMoveState();
					if(state==stackState.hang) {
						playHang();
					}
					currentStack.push(new TreeSet<>(selectedCards));
					playerPlayed.get(playersIndexOf(currentPlayer)).addAll(selectedCards);
					currentPlayer.removeAll(selectedCards);
					bai.addAll(selectedCards);
					for(Card c:selectedCards) {
						currentPlayer.ui.cards.get(c).deselect();
						cardsPanel.add(currentPlayer.ui.cards.get(c),4);
						currentPlayer.ui.cardsPane.remove(currentPlayer.ui.cards.get(c));
					}
					placeCards(currentPlayer,selectedCards);
					reformatCards(currentPlayer);
					if(DEBUG)temporaryDebugDisplay.append(selectedCards.toString());
					currentPlayer.deselectAll();
					currentPlayer.moves.clear();
//					System.out.println(state+" "+currentPlayer+"\n"+playersInGame+"\n"+players);
					waitingForMove = false;

				} else {
					//TODO error invalid move
				}
			}
		});
		play.setOpaque(false);
		play.setEnabled(false);
		pass.setOpaque(false);
		pass.setEnabled(false);
		p.ui.play=play;p.ui.pass=pass;
		p.ui.panel.add(play);p.ui.panel.add(pass);
	}

	public void handleClient(OnlinePlayer play, ClientMessage message) throws IOException {
		if(UIDEBUG)System.out.println("public void handleClient(OnlinePlayer play, ClientMessage message) throws IOException {");
		switch(message.getType()) {
		case DISCONNECT:
			
			break;
		case JOIN:
			
			break;
		case PASS:
			if(state!=stackState.none) {
				playersInPlay.remove(currentPlayer);
				nextPlayer();
				broadcast(new ServerMessage(MessageType.PLAY, getGameState()));
//				System.out.println(playersInPlay.size());
				}
			else {
				broadcast(new ServerMessage(MessageType.PASS, getGameState()));
			}
			break;
		case PLAY:
			System.out.println("Player " + play.playerID + " played: " + message.getData());
			if (!handlePlay(play,(TreeSet<Card>)message.getData())) {
				broadcast(new ServerMessage(MessageType.PASS, getGameState()));
			}
			break;
		case UPDATE:
			
			break;
		default:
			break;
		
		}
	}

	public boolean handlePlay(OnlinePlayer play, TreeSet<Card> selectedCards) throws IOException {
		if(UIDEBUG)System.out.println("public boolean handlePlay(OnlinePlayer play, TreeSet<Card> selectedCards) throws IOException {");
		if (play!=currentPlayer) return false;
		if(DEBUG)System.out.println("THIS IS PRESSED "+currentPlayer.moves+currentPlayer.current);
		if (currentPlayer.moves.contains(selectedCards)) {
			if(DEBUG)System.out.println(state);
			if(Player.getMoveState(selectedCards)!=stackState.none) state=Player.getMoveState(selectedCards);
			if(state==stackState.hang) {
				playHang();
			}
			currentStack.push(new TreeSet<>(selectedCards));
			playerPlayed.get(playersIndexOf(currentPlayer)).addAll(selectedCards);
			currentPlayer.removeAll(selectedCards);
			bai.addAll(selectedCards);
			for(Card c:selectedCards) {
				currentPlayer.ui.cards.get(c).deselect();
				cardsPanel.add(currentPlayer.ui.cards.get(c),4);
				currentPlayer.ui.cardsPane.remove(currentPlayer.ui.cards.get(c));
			}
			placeCards(currentPlayer,selectedCards);
			reformatCards(currentPlayer);
			broadcast(new ServerMessage(MessageType.PLACE, new TreeSet<Card>(selectedCards)));
			if(DEBUG)temporaryDebugDisplay.append(selectedCards.toString());
			currentPlayer.deselectAll();
			currentPlayer.moves.clear();
//			System.out.println(state+" "+currentPlayer+"\n"+playersInGame+"\n"+players);
			nextPlayer();
			broadcastUpdate();
			return true;
		} 
		return false;
	}

	private void waitForPlayerMove() throws IOException {
		if(UIDEBUG)System.out.println("private void waitForPlayerMove() throws IOException {");
		if(!robotAutoRun)waitingForMove = true;
		// Enable controls only for current player
		currentPlayer.ui.play.setEnabled(true);
		currentPlayer.ui.pass.setEnabled(true);
		// Wait for player action
		if(currentPlayer instanceof ComputerPlayer) {
			((ComputerPlayer) currentPlayer).getMove(state,(currentStack.isEmpty())?null:currentStack.peek());
		}
		while (waitingForMove) {
			try {
				Thread.sleep(250); //sleep for a fourth of a second each time
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		//as of now only pass or play changes waitingformove, so they both need this
		if(currentPlayer instanceof ComputerPlayer) {
			((ComputerPlayer) currentPlayer).play();
		}
		if (DEBUG) System.out.println("AFTER: "+currentPlayer);
		currentPlayer.ui.play.setEnabled(false);
		currentPlayer.ui.pass.setEnabled(false);
		if(DEBUG) printPlayerArrays();
		nextPlayer();
		if(DEBUG) printPlayerArrays();
	}
	
	private void printPlayerArrays() {
		if(UIDEBUG)System.out.println("private void printPlayerArrays() {");
		System.out.println(playersIndexOf(currentPlayer));
		System.out.print("playersInGame: ");
		for(Player p:playersInGame) {
			System.out.print(playersIndexOf(p)+" ");
		}
		System.out.print("\nplayersInPlay: ");
		for(Player p:playersInPlay) {
			System.out.print(playersIndexOf(p)+" ");
		}
		System.out.println();
	}
	
	private void nextPlayer() throws IOException { //get next player in game
		if(UIDEBUG)System.out.println("private void nextPlayer() throws IOException {");
		if(playersInPlay.size()<2) {
			playersInPlay=new ArrayList<Player>(playersInGame);
			state=stackState.none;
		}
		do {
			if(DEBUG) System.out.println("SWITCHING: " + playersIndexOf(currentPlayer)+" -> "+(playersIndexOf(currentPlayer) + 1) % players.size());
			if(DEBUG) System.out.println(currentPlayer+" -> "+players.get((playersIndexOf(currentPlayer) + 1) % players.size()));
			if(DEBUG) System.out.println(players);
			if(DEBUG) System.out.println(playersInPlay);
			currentPlayer = players.get((playersIndexOf(currentPlayer) + 1) % players.size());
		} while(!playersInPlay.contains(currentPlayer)||currentPlayer.isEmpty());
		winnerCheck();
		updateGameDisplay();
		highlightCurrentPlayer();
		currentPlayer.getValidMoves(state,(currentStack.isEmpty())?null:currentStack.peek());
		broadcast(new ServerMessage(MessageType.PLAY, new GameState(players,playersIndexOf(currentPlayer),state)));
		if(currentPlayer instanceof ComputerPlayer) computerPlay((ComputerPlayer)currentPlayer);
	}

	private void computerPlay(ComputerPlayer play) throws IOException {
		if(UIDEBUG)System.out.println("private void computerPlay(ComputerPlayer play) throws IOException {");
		play.getMove(state,(currentStack.isEmpty())?null:currentStack.peek());
		if(Player.getMoveState(play.current)!=stackState.none) state=Player.getMoveState(play.current);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(play.current.isEmpty()) {
			if(state!=stackState.none) {
				playersInPlay.remove(currentPlayer);
				waitingForMove = false;
//				System.out.println(playersInPlay.size());
				}
			else {
				//TODO error
			}
		}
		else{
			if(DEBUG)System.out.println(state);
			if(currentPlayer.getMoveState()!=stackState.none) state=currentPlayer.getMoveState();
			if(state==stackState.hang) {
				playHang();
			}
			currentStack.push(new TreeSet<>(play.current));
			playerPlayed.get(playersIndexOf(currentPlayer)).addAll(play.current);
			currentPlayer.removeAll(play.current);
			bai.addAll(play.current);
			for(Card c:play.current) {
				currentPlayer.ui.cards.get(c).deselect();
				cardsPanel.add(currentPlayer.ui.cards.get(c),4);
				currentPlayer.ui.cardsPane.remove(currentPlayer.ui.cards.get(c));
			}
			placeCards(currentPlayer,play.current);
			reformatCards(currentPlayer);
			broadcast(new ServerMessage(MessageType.PLACE, new TreeSet<Card>(play.current)));
			if(DEBUG)temporaryDebugDisplay.append(play.current.toString());
			currentPlayer.deselectAll();
			currentPlayer.moves.clear();
//			System.out.println(state+" "+currentPlayer+"\n"+playersInGame+"\n"+players);
			waitingForMove = false;
		}
		if(TienLen.DEBUG) {
			System.out.println("ComputerPlayer played: "+play.current);
		}

		nextPlayer();
	}
	public void reformatCards(Player p) {
		if(UIDEBUG)System.out.println("public void reformatCards(Player p) {");
		int i = playersIndexOf(p);
		boolean sides=i%2==1;
		int startX=this.startX,startY=this.startY;
		int areaWidth=(sides?startY:startX)*Utils.CARDS_PER_PLAYER-2*cardHeight;
		int overlap = Math.min(cardWidth,((sides?startY:startX)*Utils.CARDS_PER_PLAYER-cardWidth-2*cardHeight)/(Math.max(p.size(),2)-1));
		switch(i) { // 4 players, each a side
		case 0: startX+=cardHeight;
		break;
		case 1: startX = getWidth()-getInsets().left-getInsets().right-startX-cardHeight;
		startY+=cardHeight;
		break;
		case 2: startX+=cardHeight;
		startY = getHeight()-getInsets().top-getInsets().bottom-startY-cardHeight;
		break;
		case 3: startY+=cardHeight;
		break;
		}
		p.ui.cardsPane.setBounds(startX,startY,sides?cardHeight:areaWidth,sides?areaWidth:cardHeight);
		int index=0;
		for(Card c:p) {
			JCard box=p.ui.cards.get(c);
			box.setBounds(sides?0:((overlap==cardWidth?(areaWidth-cardWidth*p.size())/2:0)+index*overlap),
					!sides?0:((overlap==cardWidth?(areaWidth-cardWidth*p.size())/2:0)+index*overlap), 
							sides?cardHeight:cardWidth, sides?cardWidth:cardHeight);
			index++;
		}
	}
	
	private void placeCards(Player p, TreeSet<Card> ca) {
		if(UIDEBUG)System.out.println("private void placeCards(Player p, TreeSet<Card> ca) {");
		int i = playersIndexOf(p);
		boolean sides=i%2==1;
		int cardHeight=(int)(this.cardHeight/1.5);int cardWidth=(int)(this.cardWidth/1.5);
		int startX=this.startX*3+cardHeight/2*((currentStack.size()-1)/4),startY=this.startY*3+cardHeight/2*((currentStack.size()-1)/4);
		int overlap = ((sides?this.startY:this.startX)*Utils.CARDS_PER_PLAYER-cardWidth-2*cardHeight)/12;
		switch(i) { // 4 players, each a side
		case 0: startX+=cardHeight;
		break;
		case 1: startX = getWidth()-getInsets().left-getInsets().right-startX-cardHeight;
		startY+=cardHeight;
		break;
		case 2: startX+=cardHeight;
		startY = getHeight()-getInsets().top-getInsets().bottom-startY-cardHeight;
		break;
		case 3: startY+=cardHeight;
		break;
		}
		int index=0;
		for(Card c:ca) {
			JCard box=p.ui.cards.get(c);
			box.setBounds(startX+(sides?0:(index*overlap)),startY+(!sides?0:(index*overlap)), 
							sides?cardHeight:cardWidth, sides?cardWidth:cardHeight);
			index++;
			box.tintGrey();
		}
	}

	public void winnerCheck() { //checks if there is an empty player
		if(UIDEBUG)System.out.println("public void winnerCheck() {");
		//Because I see no scenario where there are more than 1 empty player in that array, I will do a temp variable to avoid concurrentMod
		Player winnerPlayer=null;
		for(Player p:playersInGame) {
			if (p.isEmpty()) {
				winnerPlayer=p;
			}
		}
		if(winnerPlayer!=null) {
			playersInGame.remove(winnerPlayer);
			playersInPlay.remove(winnerPlayer);
			winnerPlayer.score+=playersInGame.size();
			if(playersInGame.size()==3) winner=winnerPlayer;
			if (true) {System.out.print("scores: ");
			for(int i=0;i<Utils.MAX_PLAYERS;i++) {
				System.out.println("Player "+i+": "+players.get(i).score);
			}
			}
		}
	}

	public void handCheck() { //checking for instant wins
		if(UIDEBUG)System.out.println("public void handCheck() { ");
		for(Player p:players) {
			Player pCheck=new Player(p);
			pCheck.getMoves();
			for(TreeSet<Card>m:pCheck.moves) {
				if (m.size()==12||Player.isPigs(m,4)) {
					System.out.println("Passed HandCheck: "+m);
					winner=p;
					p.clearAll();
					p.score+=6;
					playersInGame.clear();
					playersInPlay.clear();
				}
			}
		}
	}

	private void thuiHeo(Player p) {
		if(UIDEBUG)System.out.println("private void thuiHeo(Player p) {");
		for(Card car:p) {
			p.ui.cardsPane.remove(p.ui.cards.get(car));
			if(car.rank=='2') {
				p.score-=Utils.SUIT_ORDER.get(car.suit)+1;
			}
		}
		p.getPigs();
		for (TreeSet<Card> move:p.moves) {
			if(move.size()>3){
				p.score-=4;
				break;
			}
		}
	}
	private void end() {
		if(UIDEBUG)System.out.println("private void end() {");
		if(!robotAutoRun)waitingForMove=true;
		//table1.add(temporaryDebugDisplay);
		String endText = "Scoreboard:\n";
		for(int i=0;i<Utils.MAX_PLAYERS;i++) {
			endText+=("Player "+i+": "+players.get(i).score+"\n");
		}
		temporaryDebugDisplay.setText(endText);
		cardsPanel.remove(ComputerPlayerPlay);
		table1.add(ComputerPlayerPlay);
		updateGameDisplay();
		while (waitingForMove) {
			try {
				Thread.sleep(250); //sleep for a fourth of a second each time
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		table1.remove(ComputerPlayerPlay);
		temporaryDebugDisplay.setText("");
		cardsPanel.add(ComputerPlayerPlay);
		Insets inset=getInsets();
		ComputerPlayerPlay.setBounds((getWidth()-inset.left-inset.right-50)/2,(getHeight()-inset.top-inset.bottom-50)/2,50,50);
		updateGameDisplay();
	}
	
	public void newGame() { //used only for games 2+
		if(UIDEBUG)System.out.println("public void newGame() {");
		cardsPanel.removeAll();
		cardsPanel.add(ComputerPlayerPlay);
		bai=new Deck();bai.shuffle();
		for(Player p:players) {
			p.clear();
			p.ui.cardsPane.removeAll();
			cardsPanel.add(p.ui.cardsPane);
		}
		chiaBai();
		for (Player p:players) {
			System.out.println(p);
			p.ui.panel.removeAll();
			int index=0;int i=playersIndexOf(p);int overlap = (startX*Utils.CARDS_PER_PLAYER-cardWidth-2*cardHeight)/12;
			HashMap<Card,JCard> cardButs=new HashMap<Card,JCard>();
			p.ui.cards=cardButs;
			for(Card c:p) { // Adding cards as checkboxes and those map onto a hashmap
				JCard cardBox=new JCard(c);
				cardBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if(e.getStateChange()==1) p.selectCard(c);
						else p.deselectCard(c);
					}
				});
				if(i%2==1) cardBox.rotate(90);
				cardBox.setOpaque(false);
				cardBox.setBounds(index * (i%2==0?overlap:0),index * (i%2==1?overlap:0), i%2==0?cardWidth:cardHeight, i%2==0?cardHeight:cardWidth);
				p.ui.cardsPane.add(cardBox, (i>1)?0:index);
				cardButs.put(c,cardBox);
				index++;
			}
			p.ui.panel.add(p.ui.play);
			p.ui.panel.add(p.ui.pass);
			reformatCards(p);
		}
		playersInGame=new ArrayList<Player>(players);
		playersInPlay=new ArrayList<Player>(players);
		currentPlayer=winner;
		winner=null;
		currentStack=new Stack<TreeSet<Card>>();
		state=stackState.none;
		currentPlayer.getMoves();
		updateGameDisplay();
		highlightCurrentPlayer();
	}
	private void highlightCurrentPlayer() {
		if(UIDEBUG)System.out.println("private void highlightCurrentPlayer() {");
		for(Player p:players) {
			p.ui.panel.setBackground(midColor2);
		}
		currentPlayer.ui.panel.setBackground(backColor);
	}
	
	private int playersIndexOf(Player play) { // DID YOU KNOW??? I REMOVED THIS SOME TIME BACK IN REFACTORING AND NOW HAVE TO ADD IT BACK IN BECAUSE TREESET.EQUALS STILL ALSO ACCOUNTS FOR ELEMENTS.
		if(UIDEBUG)System.out.println("private int playersIndexOf(Player play) {");
		for(int i=0;i<Utils.MAX_PLAYERS;++i) {
			if(play == players.get(i)) return i;
		}
		return -1;
	}
	private void updateGameDisplay() {
		revalidate();
		repaint();
	}
	public void broadcast(Object obj) throws IOException {
		for (Player player : players) {
			if(player instanceof OnlinePlayer)((OnlinePlayer)player).sendMessage(obj);
		}
	}
	private void broadcastUpdate() throws IOException {
		for (Player player : players) {
			if(player instanceof OnlinePlayer) {
				if(DEBUG)System.out.println("Sending to player "+playersIndexOf(player)+": "+(TreeSet<Card>)player);
				((OnlinePlayer)player).sendMessage(new ServerMessage(MessageType.UPDATE, new TreeSet<Card>(player)));
			}
		}
	}
	public void normalize(JComponent comp) {
		comp.setBackground(backColor);
		comp.setForeground(textColor);
	}
	public GameState getGameState() {
		return new GameState(players,playersIndexOf(currentPlayer),state);
	}
	public void replaceWithComputer(int play) {
		players.set(play, new ComputerPlayer(players.get(play),currentStack,playerPlayed));
	}
}

