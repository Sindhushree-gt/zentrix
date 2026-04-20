package com.example.demo.uno;
import java.util.*;

public class UnoRoom {
    public String id;
    public String p1Name;
    public String p2Name;
    public List<UnoCard> p1Hand = new ArrayList<>();
    public List<UnoCard> p2Hand = new ArrayList<>();
    public List<UnoCard> deck = new ArrayList<>();
    public List<UnoCard> discard = new ArrayList<>();
    public int turn = 1;
    public String activeColor;
    public String status = "waiting";
    public String lastMessage = "";

    public record UnoCard(int id, String color, String value) {}

    public UnoRoom(String id, String p1Name) {
        this.id = id;
        this.p1Name = p1Name;
        initDeck();
    }

    private void initDeck() {
        String[] colors = {"red", "green", "blue", "yellow"};
        String[] values = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};
        int cardId = 0;
        for (String c : colors) {
            for (String v : values) {
                deck.add(new UnoCard(cardId++, c, v));
                if (!v.equals("0")) deck.add(new UnoCard(cardId++, c, v));
            }
        }
        for (int i = 0; i < 4; i++) {
            deck.add(new UnoCard(cardId++, "wild", "Wild"));
            deck.add(new UnoCard(cardId++, "wild", "Wild Draw Four"));
        }
        Collections.shuffle(deck);
    }

    public void start() {
        for (int i = 0; i < 7; i++) {
            p1Hand.add(deck.remove(0));
            p2Hand.add(deck.remove(0));
        }
        UnoCard start = deck.remove(0);
        while (start.color().equals("wild")) {
            deck.add(start);
            Collections.shuffle(deck);
            start = deck.remove(0);
        }
        discard.add(start);
        activeColor = start.color();
        status = "active";
    }

    public synchronized void playCard(int player, int cardId, String colorOver) {
        if (turn != player) return;
        List<UnoCard> hand = player == 1 ? p1Hand : p2Hand;
        UnoCard card = hand.stream().filter(c -> c.id == cardId).findFirst().orElse(null);
        if (card == null) return;

        UnoCard top = discard.get(discard.size() - 1);
        if (card.color().equals("wild") || card.color().equals(activeColor) || card.value().equals(top.value())) {
            hand.remove(card);
            discard.add(card);
            activeColor = card.color().equals("wild") ? colorOver : card.color();
            
            // Handle effects
            boolean skip = false;
            if (card.value().equals("Skip") || card.value().equals("Reverse")) {
                skip = true; // In 2 player, Reverse acts like Skip
            } else if (card.value().equals("Draw Two")) {
                draw(player == 1 ? 2 : 1, 2);
                skip = true;
            } else if (card.value().equals("Wild Draw Four")) {
                draw(player == 1 ? 2 : 1, 4);
                skip = true;
            }

            if (hand.isEmpty()) {
                status = "finished";
                lastMessage = (player == 1 ? p1Name : p2Name) + " wins!";
            } else {
                if (!skip) turn = turn == 1 ? 2 : 1;
                lastMessage = (player == 1 ? p1Name : p2Name) + " played " + card.value();
            }
        }
    }

    public synchronized void drawCard(int player) {
        if (turn != player) return;
        draw(player, 1);
        turn = turn == 1 ? 2 : 1;
        lastMessage = (player == 1 ? p1Name : p2Name) + " drew a card";
    }

    private void draw(int player, int count) {
        List<UnoCard> hand = player == 1 ? p1Hand : p2Hand;
        for (int i = 0; i < count; i++) {
            if (deck.isEmpty()) {
                UnoCard top = discard.remove(discard.size() - 1);
                deck.addAll(discard);
                discard.clear();
                discard.add(top);
                Collections.shuffle(deck);
            }
            if (!deck.isEmpty()) hand.add(deck.remove(0));
        }
    }

    public Map<String, Object> toStateMap(int forPlayer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("p1Name", p1Name);
        map.put("p2Name", p2Name);
        map.put("status", status);
        map.put("turn", turn);
        map.put("activeColor", activeColor);
        map.put("lastMessage", lastMessage);
        map.put("topCard", discard.get(discard.size() - 1));
        map.put("deckCount", deck.size());
        
        if (forPlayer == 1) {
            map.put("hand", p1Hand);
            map.put("oppCount", p2Hand.size());
        } else if (forPlayer == 2) {
            map.put("hand", p2Hand);
            map.put("oppCount", p1Hand.size());
        } else {
            map.put("p1Count", p1Hand.size());
            map.put("p2Count", p2Hand.size());
        }
        return map;
    }
}
