package brilliance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * brilliance
 * CardDistributer
 * @author Nate
 * Feb 24, 2017
 */
public class CardDistributer {
	
	// Number of cards we hope to have in the deck
	private static final int DESIRED_DECK_SIZE = 35;

	// The maximum number of identical gem positions between any two cards
	// Value of 2 limited us to 18 cards
	private static final int MAX_OVERLAP = 3;

	public static enum Gems {
		RED("https://s3.amazonaws.com/files.component.studio/BE534356-223E-11E8-95BE-987692B40D46/red.png"),
		BLUE("https://s3.amazonaws.com/files.component.studio/BE0F6690-223E-11E8-AC1E-86955413C98C/blue.png"),
		YELLOW("https://s3.amazonaws.com/files.component.studio/BE52E55A-223E-11E8-AC1E-86955413C98C/yellow.png"),
		GREEN("https://s3.amazonaws.com/files.component.studio/BE5BD5AC-223E-11E8-95BE-987692B40D46/green.png"),
		WHITE("https://s3.amazonaws.com/files.component.studio/BE4F8A18-223E-11E8-AC1E-86955413C98C/white.png"),
		PURPLE("https://s3.amazonaws.com/files.component.studio/BE464502-223E-11E8-95BE-987692B40D46/purple.png"),
		ORANGE("https://s3.amazonaws.com/files.component.studio/BE0975D2-223E-11E8-95BE-987692B40D46/orange.png"),
		;
		
		final String csImageName;
		int[] appearances = new int[6];
		int missingFrom = 0;
		int count = 0;
		
		private Gems(String name) {
			this.csImageName = name;
		}

		public String toString() {
			int total = missingFrom;
			for (int i : appearances) {
				total+=i;
			}
			return this.name() + " " + Arrays.toString(appearances) + " count: " + total;
		}
		
		public String toCSVVariable() {
			return this.csImageName;
		}
	}
	
	public static class Card implements Comparable<Card> {
		private static final int MAX_APPEARENCES = 3;
		private static int cardCount = 0;
		int id;
		int totalAttempts = 0;
		Gems[] gemSlots = new Gems[6];
		Set<Gems> gemSet = new HashSet<>();
		int slotPointer = 0;
		
		public Card() throws Exception {
			this.id = cardCount++;
		}

		private void addGem(Gems gem) {
			gemSlots[slotPointer++] = gem;
		}
		
		public boolean addGem(Gems gem, int slot) {
			if (gem.appearances[slot] < MAX_APPEARENCES) {
				gemSlots[slot] = gem;
				gemSet.add(gem);
				return true;
			}
			return false;
		}
		
		public void clear() {
			//clear out everything
			Arrays.fill(gemSlots, null);
			gemSet.clear();
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Card o) {
			int result = 0;
			for (int i = 0; i < gemSlots.length; i++) {
				if (gemSlots[i] == null || o.gemSlots[i] == null) {
					throw new RuntimeException(spit("Found it: %s%s", this, o));
				}
				if (gemSlots[i].equals(o.gemSlots[i])) {
					result |= (1<<i);
				}
			}
			
			// now the inverse, since turning a card 180 degrees might
			// still show an overlap...
			
			for (int i = 0; i < gemSlots.length; i++) {
				Gems compareTo = o.gemSlots[5-i];
				if (gemSlots[i].equals(compareTo)) {
					result |= (1<<i);
				}
			}
			
			
			return result;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder(50);
			sb.append("#");
			sb.append(id);
			sb.append("# ");
			for (int i = 0; i < gemSlots.length; i++) {
				sb.append(" - ");
				sb.append((gemSlots[i] == null ? "NULL" : gemSlots[i].name()));
			}
			return sb.toString();
		}
	}
	
	public static Random rand = new Random();
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//List<Card> cards = randomized();
		
		List<Card> allPossibleCards = allPossibleCards();
		List<Card> mostSoFar = new ArrayList<>();
		for (int attempts = 50; attempts > 0; attempts--) {
			spit("Attempt %d", attempts);
			List<Card> attempt = sequenceSeries(allPossibleCards, mostSoFar);
			if (attempt.size() > mostSoFar.size()) {
				mostSoFar = attempt;
			}
		}
				
		showCards(mostSoFar);
		CsvExporter.export(mostSoFar);
		spit("%d cards returned", mostSoFar.size());

	}

	/**
	 * @param allPossibleCards
	 * @param mostSoFar
	 * @return
	 * @throws Exception
	 */
	private static List<Card> sequenceSeries(List<Card> allPossibleCards, List<Card> mostSoFar) throws Exception {
		int attempts = allPossibleCards.size();
		while (mostSoFar.size() < DESIRED_DECK_SIZE && attempts-- > 0) {
			for (Gems gem : Gems.values()) {
				for (int slot = 0; slot < 6; slot++) {
					gem.appearances[slot] = 0;
				}
				gem.count = 0;
			}
			List<Card> cards = sequential(allPossibleCards, attempts);
			if (cards.size() > mostSoFar.size()) {
				mostSoFar = cards;
				showCards(mostSoFar);
			}
		}
		return mostSoFar;
	}

	/**
	 * @param mostSoFar
	 */
	private static void showCards(List<Card> mostSoFar) {
		int cardNumber = 0;
		for (Card card2 : mostSoFar) {
			spit("Card %d:  %s", ++cardNumber, card2.toString());
		}
		
		for (Gems gem : Gems.values()) {
			spit("%s", gem);
		}
	}

	/**
	 * @param card2
	 */
	private static void updateGemCounts(Card card2) {
		for (int slot = 0; slot < 6; slot++) {
			Gems gem = card2.gemSlots[slot];
			gem.count++;
			gem.appearances[slot]++;
		}
	}

	private static List<Card> sequential(List<Card> allPossibleCards, int attempt) throws Exception {	
		List<Card> acceptedCards = new ArrayList<>();
		Card starterCard = allPossibleCards.get(attempt);
		acceptedCards.add(starterCard);
		updateGemCounts(starterCard);
		List<Card> availableCards = new ArrayList<>(allPossibleCards);
		Collections.shuffle(availableCards);
		for (Card oCard : availableCards) {
			boolean accepted = true;
			for (Card yCard : acceptedCards) {
				if (cardsOverlap(oCard, yCard) > MAX_OVERLAP) {
					accepted = false;
					break;
				}
			}
			if (accepted && gemCountsAcceptible(oCard)) {
				acceptedCards.add(oCard);
				updateGemCounts(oCard);
			}
		}
		
		return acceptedCards;
		
	}

	/**
	 * @return
	 * @throws Exception
	 */
	private static List<Card> allPossibleCards() throws Exception {
		List<Card> allPossibleCards = new ArrayList<>();

		for (Gems gem0 : Gems.values()) {
			List<Gems> starter = new ArrayList<>();
			starter.add(gem0);
			allPossibleCards.addAll(makeCardAtTheEnd(starter));
		}
		
		spit("%d cards constructed", allPossibleCards.size());
		return allPossibleCards;
	}
	
	public static List<Card> makeCardAtTheEnd(List<Gems> used) throws Exception {
		List<Card> cards = new ArrayList<>();
		if (used.size() == 6) {
			Card card = new Card();
			for (Gems gem : used) {
				card.addGem(gem);
			}
			cards.add(card);
		} else {
			List<Gems> gems = new ArrayList<>(used);
			for (Gems gem : Gems.values()) {
				if (!gems.contains(gem)) {
					List<Gems> available = new ArrayList<>(gems);
					available.add(gem);
					cards.addAll(makeCardAtTheEnd(available));
				}
			}
		}
		
		return cards;
	}
	
	public static boolean gemCountsAcceptible(Card card) {
		for (int slot = 0; slot < 6; slot++) {
			//for each card, there are actually 7 gem slots, including the "missing" gem slot
			//so, the number of appearances for each gem should be the deck size / 7
			if (card.gemSlots[slot].appearances[slot] >= (DESIRED_DECK_SIZE / 7)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param card
	 * @param valid
	 * @param createdCard
	 * @return
	 */
	private static int cardsOverlap(Card card, Card createdCard) {
		
		if (card.equals(createdCard)) {
			return 0;
		}
		
		// a bitmasked representation of which slots have the same gems
		// 110000 - the top rows of the two cards have the same gems
		int comparison = card.compareTo(createdCard);
		int bitCount = 0;
		for (int i = 0; i < 8; i++) {
			// just counting the 1's to see how many are hits
			if ((comparison & (1<<i)) == (1<<i)) {
				bitCount++;
			}
		}
		
		return bitCount;
	}

	public static String spit(String s, Object... objects) {
		String msg = String.format(s, objects);
		System.out.println(msg);
		return msg;
	}

}

