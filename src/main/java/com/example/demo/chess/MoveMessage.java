package com.example.demo.chess;

public class MoveMessage {
    public int[] from;
    public int[] to;
    public String promotion; // null unless pawn promotion
    public String gameStatus; // computed on frontend before sending
}
