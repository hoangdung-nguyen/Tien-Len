package cardGames;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class PlayerClient extends JFrame{
	private static final long serialVersionUID = 1L;
	private Stack<TreeSet<Card>> currentStack;
	private ArrayList<UISet> players;
	int currentPlayerID;
	int startX,startY,cardWidth,cardHeight;
	
	private Color backColor, midColor1, midColor2, textColor;
	private JPanel table,cardsPanel,table1;
	private JButton button;
	
	OnlinePlayer serverConnection;
	ObjectOutputStream out;
	ObjectInputStream in;
	boolean gameStarted;
	
	private static final String SERVER_ADDRESS = "localhost"; // Change to public IP for global play
	private static final int SERVER_PORT = 5000;
	boolean waitingForMove, connected=false;

	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		final PlayerClient[] guiHolder = new PlayerClient[1];
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				guiHolder[0] = new PlayerClient(Color.WHITE);
			}
		});
		PlayerClient thisInstance = guiHolder[0];
		thisInstance.connectToServer();
	}
	public PlayerClient(Color back){
		backColor=back;
		initializePlayArea();
		players=new ArrayList<UISet>();
		for(int i=0;i<Utils.MAX_PLAYERS;++i) players.add(new UISet());
		repaint();
		setBounds(25, 100, 1001, 1000);
		currentStack=new Stack<TreeSet<Card>>();
	}
	private void initializePlayArea(){
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
		table1=new JPanel();
		normalize(table1);
		table1.setBackground(midColor1);
		table.add(table1,BorderLayout.CENTER);
		add(table);
		add(cardsPanel);
		table.setOpaque(false);
		cardsPanel.setOpaque(false);
		setComponentZOrder(table,0);
		setComponentZOrder(cardsPanel,0);
		//table.setBounds(getBounds());
		button = new JButton("New Game");
		normalize(button);
		cardsPanel.add(button);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				// resizing panels
				Insets inset=getInsets();
				table.setBounds(inset.left,inset.top,getWidth()-inset.left-inset.right,getHeight()-inset.top-inset.bottom);
				cardsPanel.setBounds(inset.left,inset.top,getWidth()-inset.left-inset.right,getHeight()-inset.top-inset.bottom);
				button.setBounds((getWidth()-inset.left-inset.right-50)/2,(getHeight()-inset.top-inset.bottom-50)/2,50,50);
				repaint();
			}
		});
		cardsPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizeCards();
				if (connected)
				for (UISet p:players) {
					reformatCards(p);
				}
			}
		});
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				waitingForMove = false;
			}
		});
	}
	public void connectToServer() throws InterruptedException {
		while (!connected) {
			waitForPlayers();
			try {
				Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(socket.getInputStream());

				System.out.println("Connected to Tien Len Server!");
				connected = true;

				// client listening thread
				Thread listenThread = new Thread(this::handleServerMessage);
				listenThread.start();

				// debugin chat to server
				Scanner scanner = new Scanner(System.in);
			} catch (IOException e) {
				System.out.println("Server not started or connection failed.");
				Thread.sleep(1000); // Retry after short delay
			}
		}
	}
	private void waitForPlayers() {
		waitingForMove = true;
		System.out.println("WAITING");
		while (waitingForMove) {
			try {
				Thread.sleep(1000); // Prevent CPU overuse
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	private void handleServerMessage() {
		try {
			Object response;
			while ((response = in.readObject()) != null) {
				System.out.println("SERVER SENT: " + response);
				if (response instanceof OnlinePlayer) {
					serverConnection = (OnlinePlayer) response;
					System.out.println("Received OnlinePlayer from server: ID " + serverConnection.playerID + serverConnection);
				}
				else if (response instanceof ServerMessage){
					out.reset();
					switch(((ServerMessage) response).getType()) {
					case DISCONNECT:
						break;
					case JOIN:
						if(TienLen.DEBUG)System.out.println("GAME HAS STARTED");
						GameState startingSetup = (GameState) ((ServerMessage)response).getData();
						for (int i = 0;i<players.size();++i) {
							if(i==serverConnection.playerID) {
								serverConnection.ui = players.get(i);
							}
							players.get(i).cardsNum = startingSetup.handSizes.get(i);
						}
						SwingUtilities.invokeAndWait(this::startGameSetup);
						break;
					case PASS:
						if(TienLen.DEBUG)System.out.println("DUMBASS TRY AGAIN");
						break;
					case PLAY: //get state
						GameState gameS = (GameState) ((ServerMessage)response).getData();
						if(TienLen.DEBUG)System.out.println("Server updated state"+gameS);
						for (int i = 0;i<players.size();++i) {
							if(players.get(i).cardsNum != gameS.handSizes.get(i)) {
								players.get(i).cardsNum = gameS.handSizes.get(i);
								reformatCards(players.get(i));
							}
						}
						this.currentPlayerID = gameS.currentPlayerID;
						SwingUtilities.invokeLater(this::highlightPlayer);
						break;
					case UPDATE: //get hand
						serverConnection.deselectAll();
						serverConnection.moves.clear();
						if(TienLen.DEBUG)System.out.println("Got hand: "+(TreeSet<Card>) ((ServerMessage)response).getData());
						serverConnection.clear();
						serverConnection.addAll((TreeSet<Card>) ((ServerMessage)response).getData());
						break;
					case PLACE:
						TreeSet<Card> cards = (TreeSet<Card>) ((ServerMessage)response).getData();
						if(TienLen.DEBUG)System.out.println("Got place: "+cards);
						currentStack.push(new TreeSet<>(cards));
						placeCards(currentPlayerID,cards);
						break;
					default:
						break;
					
					}
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void startGameSetup() {
		for(int i=0;i<players.size();i++) initializePlayerArea(i);
		// I would love to put these in initPlayArea, but these need to be after the cards' bounds are set so they can all run componentResized when this is set.
		for (UISet p:players) {
			reformatCards(p);
		}
	}

	private void resizeCards() { //resizes cards on player hands.
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

	private void initializePlayerPanels(int i) {
		UISet p=players.get(i);
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
		p.panel=temp;int index = 0;p.cardsPane=new JPanel();p.cardsPane.setLayout(null);p.cardsPane.setOpaque(false);
		normalize(temp);
		HashMap<Card,JCard> cardButs=new HashMap<Card,JCard>();
		if(i==serverConnection.playerID)
			for(Card c:serverConnection) { // Adding cards as checkboxes and those map onto a hashmap
				JCard box=new JCard(c);
				box.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if(e.getStateChange()==1) serverConnection.selectCard(c);
						else serverConnection.deselectCard(c);
					}
				});
				box.setOpaque(false);
				box.setBounds(index * (i%2==0?overlap:0),index * (i%2==1?overlap:0), i%2==0?cardWidth:cardHeight, i%2==0?cardHeight:cardWidth);
				p.cardsPane.add(box, (i>1)?0:index);
				if(i%2==1) box.rotate(90);
				cardButs.put(c,box);
				index++;
			}
		else {
			for(;index<p.cardsNum;++index) { // Adding cards as checkboxes and those map onto a hashmap
				JCard box=new JCard(Utils.LOWEST_CARD);
				box.setOpaque(false);
				box.setBounds(index * (i%2==0?overlap:0),index * (i%2==1?overlap:0), i%2==0?cardWidth:cardHeight, i%2==0?cardHeight:cardWidth);
				p.cardsPane.add(box, (i>1)?0:index);
				if(i%2==1) box.rotate(90);
			}
		}
		cardsPanel.add(p.cardsPane,i);
		p.cardsPane.setBounds(startX,startY,i%2==0?areaWidth:cardHeight,i%2==1?areaWidth:cardHeight);
		p.cards=cardButs;
	}

	private void initializePlayerArea(int i) {
		UISet p=players.get(i);
		initializePlayerPanels(i);
		if(i==serverConnection.playerID) {
			if(TienLen.DEBUG) System.out.println("Client making buttons");
			JButton pass=new JButton("Pass");
			normalize(pass);
			pass.addActionListener(new ActionListener(){ // The pass button
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						out.writeObject(new ClientMessage(MessageType.PASS,new TreeSet<Card>(serverConnection.current)));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			JButton play=new JButton("Play");
			normalize(play);
			play.addActionListener(new ActionListener(){ // The play button
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						out.writeObject(new ClientMessage(MessageType.PLAY,new TreeSet<Card>(serverConnection.current)));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			play.setOpaque(false);
			play.setEnabled(false);
			pass.setOpaque(false);
			pass.setEnabled(false);
			p.play=play;p.pass=pass;
			p.panel.add(play);p.panel.add(pass);
			if(TienLen.DEBUG)System.out.println("Made buttons.");
		}
	}
	

	
	public void reformatCards(UISet onlinePlayer) {
		int i = players.indexOf(onlinePlayer);
		if(TienLen.DEBUG) System.out.println("Reformating: Player "+i);
		boolean sides=i%2==1;
		int startX=this.startX,startY=this.startY;
		int areaWidth=(sides?startY:startX)*Utils.CARDS_PER_PLAYER-2*cardHeight;
		int overlap = Math.min(cardWidth,((sides?startY:startX)*Utils.CARDS_PER_PLAYER-cardWidth-2*cardHeight)/(Math.max(onlinePlayer.cardsNum,2)-1));
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
		onlinePlayer.cardsPane.setBounds(startX,startY,sides?cardHeight:areaWidth,sides?areaWidth:cardHeight);
		int index=0;
		Component[] components = onlinePlayer.cardsPane.getComponents();
	    for (int j = components.length - 1; j >= onlinePlayer.cardsNum; j--) {
	    	onlinePlayer.cardsPane.remove(components[j]);
	    }
		for(Component c:onlinePlayer.cardsPane.getComponents()) if(c instanceof JCard) {
			c.setBounds(sides?0:((overlap==cardWidth?(areaWidth-cardWidth*onlinePlayer.cardsNum)/2:0)+index*overlap),
					!sides?0:((overlap==cardWidth?(areaWidth-cardWidth*onlinePlayer.cardsNum)/2:0)+index*overlap), 
							sides?cardHeight:cardWidth, sides?cardWidth:cardHeight);
			index++;
		}
	}

	void placeCards(int i, TreeSet<Card> ca) {
		if(TienLen.DEBUG) System.out.println("Placing: "+ ca +" in Player "+i);
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
			JCard box;
			if(i==serverConnection.playerID) box = players.get(i).cards.get(c);
			else {
				box=new JCard(c);
				if(sides) box.rotate(90);
			}
			box.setBounds(startX+(sides?0:(index*overlap)),startY+(!sides?0:(index*overlap)), 
							sides?cardHeight:cardWidth, sides?cardWidth:cardHeight);
			index++;
			cardsPanel.add(box, 4);
			box.tintGrey();
		}
	}
	private void highlightPlayer() {
		for(UISet p:players) {
			p.panel.setBackground(midColor2);
		}
		players.get(currentPlayerID).panel.setBackground(backColor);
		UISet p = players.get(serverConnection.playerID);
		if(currentPlayerID == serverConnection.playerID) {
			p.play.setEnabled(true);
			p.pass.setEnabled(true);
		}
		else{
			p.play.setEnabled(false);
			p.pass.setEnabled(false);			
		}
		updateGameDisplay();
	}
	private void updateGameDisplay() {
		revalidate();
		repaint();
	}
	public void normalize(JComponent comp) {
		comp.setBackground(backColor);
		comp.setForeground(textColor);
	}
	public void playHang() {
		//TODO Celebration!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	}
}
class OnlinePlayer extends Player implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final transient ObjectInputStream in;
	final transient ObjectOutputStream out;
	private final transient Socket socket;
	PlayerClient playerGUIHolder;
	TienLen server; // monsier container
	int currentPlayerID;
	int playerID;
	stackState currentState;
	
	public OnlinePlayer(Socket socket, int id, TienLen server) throws IOException {
		this.socket = socket;
		this.playerID = id;
		this.out = new ObjectOutputStream(socket.getOutputStream());
		this.out.flush();
		this.in = new ObjectInputStream(socket.getInputStream());
		this.server = server;
	}
	public void addGUIConnection(PlayerClient ui) {
		playerGUIHolder = ui;
	}
// TODO This is where server listens 
	@Override
	public void run() {
		try {
			Object obj;
			while ((obj = in.readObject()) != null) {
				if (obj instanceof ClientMessage) {
					ClientMessage message = (ClientMessage) obj;
					System.out.println("SERVER RECEIVED: " + message);
					server.handleClient(this,message);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Player " + playerID + " disconnected.");
			server.replaceWithComputer(playerID);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isTurn() {
		return currentPlayerID == playerID;
	}
	public void sendMessage(Object obj) throws IOException {
		try {
			out.writeObject(obj);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
