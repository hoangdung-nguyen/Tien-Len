package cardGames;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

public class JCard extends JToggleButton {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static BufferedImage cardSheet;
	static {
        try {
            cardSheet = ImageIO.read(new File("cards.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	static final int CARD_WIDTH = cardSheet.getWidth()/14;
    static final int CARD_HEIGHT = cardSheet.getHeight()/4;
	private ImageIcon cardImage;
	private BufferedImage master;
	private BufferedImage selected;
	private BufferedImage currentImage;
	ItemListener item;
	public boolean isSelected=false;
	Card card;
    public JCard(Card c) {
    	card=c;
    	this.cardImage = new ImageIcon("cards.png");
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
		master=cardSheet.getSubimage(CARD_WIDTH*Utils.RANK_ORDER.get(card.rank), CARD_HEIGHT*Utils.SUIT_ORDER.get(card.suit), CARD_WIDTH, CARD_HEIGHT);
        selected=tint(master,new Color(178,178,178,255));
		currentImage=master;
        setPreferredSize(new Dimension(100,100));
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
            	setIcon();
            }
        });
        item= new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==1) isSelected=true;
				else isSelected=false;
				setIcon();
			}
		};
        addItemListener(item);
    }
    public static double getRatio() {
    	return (double)CARD_HEIGHT/CARD_WIDTH;
    }
    public void tintGrey() {
    	master=tint(master,new Color(178,178,178,255));
    	currentImage=tint(currentImage,new Color(178,178,178,255));
    	setIcon();
    }
    public void deselect() {
    	this.removeItemListener(item);
    	isSelected=false;
    	setIcon();
    	setFocusable(false);
    }
    private void setIcon(){
    	int w=getWidth();
    	int h=getHeight();
    	if(h<2&&w<2)return;
    	Image scaled;
    	if(isSelected) scaled = selected.getScaledInstance((w>=h)?-1:w, (w<h)?-1:h, Image.SCALE_SMOOTH);
    	else scaled = currentImage.getScaledInstance((w>=h)?-1:w, (w<h)?-1:h, Image.SCALE_SMOOTH);
        super.setIcon(new ImageIcon(scaled));
    }
    public BufferedImage tint(BufferedImage image, Color color) {
    	BufferedImage out=new BufferedImage(image.getWidth(), image.getHeight(),image.getType());
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color pixelColor = new Color(image.getRGB(x, y), true);
                int r = Math.min((pixelColor.getRed() + color.getRed()) / 2,pixelColor.getRed());
                int g = Math.min((pixelColor.getGreen() + color.getGreen()) / 2,pixelColor.getGreen());
                int b = Math.min((pixelColor.getBlue() + color.getBlue()) / 2,pixelColor.getBlue());
                int a = pixelColor.getAlpha();
                int rgba = (a << 24) | (r << 16) | (g << 8) | b;
                out.setRGB(x, y, rgba);
            }
        }
        return out;
    }
    @Override
    public Dimension getPreferredSize() {
    	double widthRatio=getParent().getWidth()/4.0/currentImage.getWidth();
    	double heightRatio=getParent().getHeight()/4.0/currentImage.getHeight();
    	System.out.println(":( "+widthRatio+" "+heightRatio);
    	return master == null
                ? new Dimension(100, 200)
                : widthRatio>heightRatio
                	? new Dimension((int)Math.floor(currentImage.getWidth()*widthRatio), (int)Math.floor(currentImage.getHeight()*widthRatio))
                	: new Dimension((int)Math.floor(currentImage.getWidth()*heightRatio), (int)Math.floor(currentImage.getHeight()*heightRatio));
    }
    public void rotate(double angle) {

        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = master.getWidth();
        int h = master.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        currentImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = currentImage.createGraphics();
        BufferedImage temp= new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d2= temp.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(master, 0, 0, this);
        g2d.dispose();
        g2d2.setTransform(at);
        g2d2.drawImage(selected, 0, 0, this);
        g2d2.dispose();
        selected=temp;
        Image scaled = currentImage.getScaledInstance((w>=h)?-1:w, (w<h)?-1:h, Image.SCALE_SMOOTH);
        setIcon(new ImageIcon(scaled));
    }
    public void setCardImage(String imagePath) {
        this.cardImage = new ImageIcon(imagePath);
        setIcon(cardImage);
    }

    public ImageIcon getCardImage() {
        return cardImage;
    }

    public void addClickListener(ActionListener listener) {
        this.addActionListener(listener);
    }
}
