package game.ninemensmorris.Models;

public interface MoveEvaluationFunction {
	public int evaluate(BoardState boardState, Move move);
}
