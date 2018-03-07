package brilliance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	
	public static enum Gems {
		RED,
		BLUE,
		YELLOW,
		GREEN,
		WHITE,
		PINK,
		ORANGE,
		;
		
		int[] appearences = new int[6];
		int missingFrom = 0;
		int count = 0;
		
		public String toString() {
			int total = missingFrom;
			for (int i : appearences) {
				total+=i;
			}
			return this.name() + " " + Arrays.toString(appearences) + " count: " + total;
		}
	}
	
	public static class Card implements Comparable<Card> {
		/**
		 * 
		 */
		private static final int MAX_APPEARENCES = 3;
		/**
		 * 
		 */
		private static final int MAX_ATTEMPTS = 20000;
		private static int cardCount = 0;
		int id;
		int totalAttempts = 0;
		Gems[] gemsArray = new Gems[6];
		Set<Gems> gemSet = new HashSet<>();
		int slotPointer = 0;
		
		public Card() throws Exception {
			this.id = cardCount++;
		}

		/**
		 * @throws Exception
		 */
		private void init() throws Exception {
			List<Gems> gemsLeft = new ArrayList<>(Arrays.asList(Gems.values()));
			Collections.shuffle(gemsLeft);
			populate(gemsLeft);
		}

		/**
		 * @param gemsLeft
		 * @throws Exception
		 */
		private void populate(List<Gems> gemsLeft) throws Exception {
			Gems gem;
			for (int slot = 0; slot < 6; slot++) {
				int attempts = 0;
				while (gemsArray[slot] == null && attempts++ <= MAX_ATTEMPTS) {
					gem = gemsLeft.get(0);
					if (addGem(gem, slot)) {
						gemsLeft.remove(gem);
					}
				}
				totalAttempts+= attempts;
				if (attempts >= MAX_ATTEMPTS) {
					throw new Exception("Attemps exceeded max attempts.");
				}
			}
		}
		
		public void addGem(Gems gem) {
			gemsArray[slotPointer++] = gem;
		}
		
		public boolean addGem(Gems gem, int slot) {
			if (gem.appearences[slot] < MAX_APPEARENCES) {
				gemsArray[slot] = gem;
				gemSet.add(gem);
				return true;
			}
			return false;
		}
		
		public void clear() {
			//clear out everything
			Arrays.fill(gemsArray, null);
			gemSet.clear();
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Card o) {
			int result = 0;
			for (int i = 0; i < gemsArray.length; i++) {
				if (gemsArray[i] == null || o.gemsArray[i] == null) {
					throw new RuntimeException(spit("Found it: %s%s", this, o));
				}
				if (gemsArray[i].equals(o.gemsArray[i])) {
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
			for (int i = 0; i < gemsArray.length; i++) {
				sb.append(" - ");
				sb.append((gemsArray[i] == null ? "NULL" : gemsArray[i].name()));
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
				
		int cardNumber = 0;
		for (Card card2 : mostSoFar) {
			spit("Card %d:  %s", ++cardNumber, card2.toString());
		}
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
		while (mostSoFar.size() < 21 && attempts-- > 0) {
			List<Card> cards = sequential(allPossibleCards, attempts);
			if (cards.size() > mostSoFar.size()) {
				mostSoFar = cards;
				int cardNumber = 0;
				for (Card card2 : mostSoFar) {
					spit("Card %d:  %s", ++cardNumber, card2.toString());
				}
				
				for (Gems gem : Gems.values()) {
					spit("%s", gem);
				}
			}
			for (Gems gem : Gems.values()) {
				for (int slot = 0; slot < 6; slot++) {
					gem.appearences[slot] = 0;
				}
				gem.count = 0;
			}
		}
		return mostSoFar;
	}

	/**
	 * @param card2
	 */
	private static void updateGemCounts(Card card2) {
		for (int slot = 0; slot < 6; slot++) {
			Gems gem = card2.gemsArray[slot];
			gem.count++;
			gem.appearences[slot]++;
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
				if (cardsOverlap(oCard, yCard) > 2) {
					accepted = false;
					break;
				}
			}
			if (accepted && cardIsAcceptible(oCard)) {
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
		
		for (Card card : allPossibleCards) {
			spit("%s", card);
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
	
	public static boolean cardIsAcceptible(Card card) {
		for (int slot = 0; slot < 6; slot++) {
			if (card.gemsArray[slot].appearences[slot] > 2) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 */
	private static List<Card> randomized() {
		List<Card> randomizedCards = new ArrayList<>();
		
		int attempts = 0;
		int maxAttempts = 5000000;
		Card card;
		while (randomizedCards.size() < 21 && attempts++ < maxAttempts) {
			if (attempts%(maxAttempts/10) == 0) {
				spit("Attempt... %d", attempts);
			}
			try {
				card = new Card();
				card.init();
				boolean valid = true;
				for (Card createdCard : randomizedCards) {
					try {
						valid = cardsOverlap(card, createdCard) < 2;
						if (!valid) {
							break;
						}
					} catch (RuntimeException e) {
						spit("Card attempt: %d", attempts);
						spit("Created cards: \n%s", randomizedCards);
						spit("New card: %s", card);
						spit("Gems:\n%s\n%s\n%s\n%s\n%s\n%s\n%s", (Object[])Gems.values());
						e.printStackTrace();
						System.exit(0);
					}
					
				}
				
				if (valid) {
					randomizedCards.add(card);
					
					updateGemCounts(card);
					
				} else {
					card.clear();
				}
			} catch (Exception e1) {
				//spit("Ran out of options: %s.", e1.getMessage());
			}
		}
		
		spit("Attempts: %d", attempts);
		return randomizedCards;
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
		
		int comparison = card.compareTo(createdCard);
		int bitCount = 0;
		for (int i = 0; i < 8; i++) {
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

