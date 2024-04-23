package game.ninemensmorris.Gui;

import game.ninemensmorris.Models.Move;

public interface MoveExecutorCallback {
	public void makeMove(Move move);
	public void terminate();
}
